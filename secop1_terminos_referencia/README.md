# Términos de referencia SECOP I: 5 proyectos de diseño de infraestructura pública

Cinco concursos de méritos abiertos de **consultoría para estudios y diseños**, cada uno de un tipo de infraestructura distinto. Se tomaron del conjunto de datos abiertos *SECOP I - Procesos de Compra Pública* (datos.gov.co, `f789-7hwg`).

| # | Tipo | Constancia | Entidad | Año | Valor contrato (COP) | Proceso |
|---|------|-----------|---------|-----|---------------------:|---------|
| 1 | Vial (carretera) | 19-15-10136670 | Gobernación de Norte de Santander: vía Astilleros–Tibú | 2019 | 3.779.294.799 | [SECOP I](https://www.contratos.gov.co/consultas/detalleProceso.do?numConstancia=19-15-10136670) |
| 2 | Puente vehicular | 18-15-8726682 | Alcaldía de Vistahermosa (Meta): puente sobre el caño Sardinata | 2018 | 119.274.236 | [SECOP I](https://www.contratos.gov.co/consultas/detalleProceso.do?numConstancia=18-15-8726682) |
| 3 | Acueducto y alcantarillado | 25-15-14534758 | Alcaldía de Mitú (Vaupés): plan maestro | 2025 | 3.068.500.586 | [SECOP I](https://www.contratos.gov.co/consultas/detalleProceso.do?numConstancia=25-15-14534758) |
| 4 | Educativa (colegio) | 24-15-14213567 | Alcaldía de Chaparral (Tolima): I.E. Técnica Camacho Angarita | 2024 | 498.510.213 | [SECOP I](https://www.contratos.gov.co/consultas/detalleProceso.do?numConstancia=24-15-14213567) |
| 5 | Hospitalaria (salud) | 21-15-12336659 | E.S.E. Hospital San Félix de La Dorada (Caldas): reposición | 2021 | 1.295.358.500 | [SECOP I](https://www.contratos.gov.co/consultas/detalleProceso.do?numConstancia=21-15-12336659) |

El detalle completo (objeto, contratista, estado y modalidad) está en `procesos.csv`.

Notas:
- En el proceso 2, el campo `cuantia_proceso` de datos abiertos dice 120.000.000.000, un error de digitación de la entidad. El valor del contrato es 119.274.236.
- El proceso 5 es la segunda publicación (12-2021). La primera, 21-15-12212857, quedó *Descartada*.

## Descarga de los documentos

Los términos de referencia de un concurso de méritos en SECOP I están repartidos en el pliego de condiciones definitivo, los estudios previos y los anexos técnicos. El script descarga los documentos de cada proceso desde contratos.gov.co:

```bash
python3 descargar_tdr.py              # todos los documentos de los 5 procesos
python3 descargar_tdr.py --solo-tdr   # solo pliegos, estudios previos y anexos
python3 descargar_tdr.py 25-15-14534758   # un proceso concreto
```

Los archivos quedan en `documentos/<constancia>_<tipo>/`. Cada carpeta lleva un `indice_documentos.tsv` que marca con `TDR` los documentos que forman los términos de referencia. El script solo usa la librería estándar de Python 3.
