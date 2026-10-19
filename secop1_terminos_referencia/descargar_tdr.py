#!/usr/bin/env python3
"""Descarga los documentos (términos de referencia / pliegos, estudios previos,
anexos técnicos, etc.) de los procesos de SECOP I listados en procesos.csv.

Uso:
    python3 descargar_tdr.py                 # todos los procesos de procesos.csv
    python3 descargar_tdr.py 19-15-10136670  # solo uno o varios números de constancia
    python3 descargar_tdr.py --solo-tdr      # solo pliegos / estudios previos / anexos

Requiere solo la librería estándar de Python 3.8+ y acceso a www.contratos.gov.co.
"""
import csv
import html
import http.cookiejar
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://www.contratos.gov.co"
DETALLE = BASE + "/consultas/detalleProceso.do?numConstancia={}"
AQUI = Path(__file__).resolve().parent
SALIDA = AQUI / "documentos"

# Palabras que identifican los documentos que forman los términos de referencia
# en un concurso de méritos de SECOP I.
CLAVES_TDR = (
    "pliego", "termino", "referencia", "estudio previo", "estudios previos",
    "anexo", "apendice", "alcance", "especificacion", "requerimiento",
    "aviso de convocatoria", "proyecto de pliego",
)

opener = urllib.request.build_opener(
    urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
opener.addheaders = [("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) secop1-tdr/1.0")]


def sin_tildes(texto):
    texto = unicodedata.normalize("NFKD", texto)
    return "".join(c for c in texto if not unicodedata.combining(c)).lower()


def nombre_archivo(texto, max_len=90):
    texto = re.sub(r"[^\w\-. ]+", "", sin_tildes(texto)).strip().replace(" ", "_")
    return texto[:max_len] or "documento"


def obtener(url, intentos=4):
    for i in range(intentos):
        try:
            with opener.open(url, timeout=120) as r:
                return r.read(), r.headers
        except Exception as e:  # red inestable del portal: reintento con espera
            if i == intentos - 1:
                raise
            espera = 2 ** (i + 1)
            print(f"    reintento en {espera}s ({e})")
            time.sleep(espera)


def documentos_del_proceso(constancia):
    """Devuelve [(descripcion, url)] con los archivos publicados en el proceso."""
    cuerpo, _ = obtener(DETALLE.format(constancia))
    pagina = cuerpo.decode("latin-1", errors="replace")
    docs, vistos = [], set()
    # Cada documento aparece en una fila <tr> con su nombre y un enlace a
    # /archivospuc1/... (href directo o window.open('...')).
    for fila in re.findall(r"<tr[^>]*>(.*?)</tr>", pagina, flags=re.S | re.I):
        for ruta in re.findall(r"""['"]((?:https?://[^'"]+)?/?archivospuc\d*/[^'"]+)['"]""", html.unescape(fila), flags=re.I):
            url = urllib.parse.urljoin(BASE + "/", ruta)
            if url in vistos:
                continue
            vistos.add(url)
            celdas = [re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", c))).strip()
                      for c in re.findall(r"<td[^>]*>(.*?)</td>", fila, flags=re.S | re.I)]
            descripcion = " - ".join(c for c in celdas if c)[:200] or Path(url).name
            docs.append((descripcion, url))
    return docs


def es_tdr(descripcion):
    d = sin_tildes(descripcion)
    return any(k in d for k in CLAVES_TDR)


def main(args):
    solo_tdr = "--solo-tdr" in args
    pedidos = [a for a in args if not a.startswith("--")]
    with open(AQUI / "procesos.csv", encoding="utf-8") as f:
        procesos = [p for p in csv.DictReader(f) if not pedidos or p["numero_constancia"] in pedidos]

    for p in procesos:
        constancia = p["numero_constancia"]
        carpeta = SALIDA / f"{constancia}_{nombre_archivo(p['tipo_infraestructura'], 40)}"
        carpeta.mkdir(parents=True, exist_ok=True)
        print(f"\n== {constancia} | {p['tipo_infraestructura']} | {p['entidad']}")
        try:
            docs = documentos_del_proceso(constancia)
        except Exception as e:
            print(f"  ERROR leyendo el detalle del proceso: {e}")
            continue
        if not docs:
            print("  No se encontraron documentos (revisa la página manualmente):", p["url_secop"])
            continue
        indice = []
        for n, (descripcion, url) in enumerate(docs, 1):
            marca = "TDR" if es_tdr(descripcion) else "otro"
            indice.append(f"{n:02d}\t{marca}\t{descripcion}\t{url}")
            if solo_tdr and marca != "TDR":
                continue
            extension = Path(urllib.parse.urlparse(url).path).suffix or ".pdf"
            destino = carpeta / f"{n:02d}_{nombre_archivo(descripcion)}{extension}"
            if destino.exists() and destino.stat().st_size > 0:
                print(f"  [ya existe] {destino.name}")
                continue
            try:
                datos, _ = obtener(url)
                destino.write_bytes(datos)
                print(f"  [{marca}] {destino.name} ({len(datos) // 1024} KB)")
            except Exception as e:
                print(f"  ERROR descargando {url}: {e}")
        (carpeta / "indice_documentos.tsv").write_text(
            "n\ttipo\tdescripcion\turl\n" + "\n".join(indice) + "\n", encoding="utf-8")
    print(f"\nListo. Documentos en: {SALIDA}")


if __name__ == "__main__":
    main(sys.argv[1:])
