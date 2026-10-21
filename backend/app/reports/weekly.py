"""Weekly report of one child: the "Bitácora de filtro diario" form, one column per weekday.

Pure rendering: it takes already loaded logs and returns the PDF bytes, so it never touches the database."""
from collections.abc import Callable
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta

from fpdf import FPDF

from app.database.models import Amount
from app.schemas.logs import DailyLogOut

DAYS = ("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
AMOUNTS = {Amount.NONE: "Nada", Amount.SOME: "Poco", Amount.ALL: "Todo", Amount.ASKED_FOR_MORE: "Pidió más"}
GROUPS = {"L": "Lactantes", "M": "Maternales", "P": "Preescolares"}

LABEL_W = 56
LINE_H = 3.6
DARK = (90, 90, 90)
LIGHT = (220, 220, 220)


@dataclass(frozen=True)
class ReportChild:
    full_name: str
    estancia: str
    salon: str | None
    group_type: str | None


def week_range(year: int, week: int) -> tuple[date, date]:
    """Monday and Friday of an ISO week. ValueError if the week doesn't exist in that year."""
    monday = date.fromisocalendar(year, week, 1)
    return monday, monday + timedelta(days=4)


# --- cell values --------------------------------------------------------------------------

def _yn(value: bool | None, na: str = "-") -> str:
    return na if value is None else ("Sí" if value else "No")


def _hour(value: time | None) -> str:
    return value.strftime("%H:%M hrs") if value else "-"


def _text(value: str | None) -> str:
    return value.strip() if value and value.strip() else "-"


def _with_detail(flag: bool | None, detail: str | None) -> str:
    return f"Sí: {detail.strip()}" if flag and detail and detail.strip() else _yn(flag)


def _injury(body_part: str | None, description: str | None) -> str:
    parts = [p.strip() for p in (body_part, description) if p and p.strip()]
    return " - ".join(parts) if parts else "NA"


def _amount(value: Amount | None) -> str:
    return AMOUNTS.get(value, "-") if value else "-"


Row = tuple[str, Callable[[DailyLogOut], str]]

CHECK_IN_ROWS: list[Row] = [
    ("Asistente al cuidado de la niña o niño", lambda l: _text(l.caregiver_name)),
    ("Hora de entrada", lambda l: _hour(l.check_in.check_in_time)),
    ("Nombre de quien entrega a la niña o niño", lambda l: _text(l.check_in.dropped_off_by_name)),
    ("Personal de la E.I. que recibe", lambda l: _text(l.check_in.received_by_staff_name)),
    ("La niña o niño está enferma(o)", lambda l: _yn(l.check_in.is_sick)),
    ("Se dejó medicamento con receta médica", lambda l: _yn(l.check_in.medication_left_with_prescription, "No aplica")),
    ("Ingresa con síntomas de enfermedad", lambda l: _with_detail(l.check_in.arrives_with_symptoms,
                                                                    l.check_in.symptoms_description)),
    ("Se presentó limpia(o)", lambda l: _yn(l.check_in.arrived_clean)),
    ("Trajo su mochila completa", lambda l: _yn(l.check_in.brought_complete_backpack)),
    ("Se presentó en buen estado físico", lambda l: _yn(l.check_in.good_physical_condition)),
    ("Lesión al ingresar (parte del cuerpo y lesión)", lambda l: _injury(l.check_in.injury_body_part,
                                                                        l.check_in.injury_description)),
    ("Observaciones para madres, padres o personas autorizadas", lambda l: _text(l.check_in.parents_notes)),
]

CHECK_OUT_ROWS: list[Row] = [
    ("Se entrega en buen estado físico", lambda l: _yn(l.check_out.released_in_good_physical_condition)),
    ("Lesión al egresar (parte del cuerpo y lesión)", lambda l: _injury(l.check_out.injury_body_part,
                                                                       l.check_out.injury_description)),
    ("Hora de salida", lambda l: _hour(l.check_out.check_out_time)),
    ("Nombre de quien recibe a la niña o niño", lambda l: _text(l.check_out.picked_up_by_name)
        + (" (persona autorizada)" if l.check_out.pickup_authorization_id else "")),
    ("Se entregó limpia(o)", lambda l: _yn(l.check_out.released_clean)),
    ("Se entregó con pertenencias completas", lambda l: _yn(l.check_out.belongings_complete)),
    ("Observaciones", lambda l: _text(l.check_out.notes)),
    ("Personal de la E.I. que entrega", lambda l: _text(l.check_out.released_by_staff_name)),
]

STAY_GROUPS: list[tuple[str, list[Row]]] = [
    ("Alimentación", [
        ("Desayuno", lambda l: _amount(l.stay.feeding_breakfast)),
        ("Colación", lambda l: _amount(l.stay.feeding_snack)),
        ("Comida", lambda l: _amount(l.stay.feeding_lunch)),
        ("Colación vespertina", lambda l: _amount(l.stay.feeding_afternoon_snack)),
        ("Observaciones generales", lambda l: _text(l.stay.feeding_notes)),
    ]),
    ("Descanso", [
        ("¿La niña o niño durmió?", lambda l: _yn(l.stay.slept)),
        ("Tiempo de siesta", lambda l: f"{l.stay.nap_minutes} minutos" if l.stay.nap_minutes is not None else "-"),
        ("Comentarios", lambda l: _text(l.stay.rest_comments)),
    ]),
    ("Funciones excretoras", [
        ("Control de esfínteres: avisó", lambda l: _yn(l.stay.toileting_notice, "No aplica")),
        ("Número de veces pipí", lambda l: str(l.stay.pee_count)),
        ("Número de veces popó", lambda l: str(l.stay.poop_count)),
        ("Observaciones", lambda l: _text(l.stay.toileting_notes)),
    ]),
    ("Estado de ánimo", [
        ("Durante su estadía estuvo", lambda l: _text(l.stay.mood)),
        ("¿Lloró?", lambda l: _yn(l.stay.cried)),
        ("¿Peleó?", lambda l: _yn(l.stay.fought)),
        ("¿Participó en las actividades?", lambda l: _yn(l.stay.participated)),
        ("Observaciones", lambda l: _text(l.stay.mood_notes)),
    ]),
    ("Accidentes", [
        ("¿Tuvo algún accidente? Descripción", lambda l: _with_detail(l.stay.had_accident,
                                                                     l.stay.accident_description)),
        ("Folio de reporte de accidentes", lambda l: _text(l.stay.accident_report_folio)),
    ]),
    ("Salud", [
        ("¿Presentó algún problema de salud? ¿Cuál?", lambda l: _with_detail(l.stay.health_issue,
                                                                            l.stay.health_issue_description)),
        ("Atención proporcionada", lambda l: _text(l.stay.care_provided)),
        ("Observaciones generales", lambda l: _text(l.stay.general_health_notes)),
    ]),
]


def _activities(log: DailyLogOut) -> str:
    if not log.activities:
        return "-"
    lines = []
    for a in log.activities:
        photos = len(a.images)
        lines.append(f"- {a.name}" + (f" ({photos} foto{'s' if photos != 1 else ''})" if photos else ""))
    return "\n".join(lines)


# --- layout -------------------------------------------------------------------------------

def _latin1(text: str) -> str:
    """The core fonts are latin-1: Spanish is fine, anything else (emoji) becomes '?'."""
    return text.encode("latin-1", "replace").decode("latin-1")


class _Report(FPDF):
    def __init__(self, child: ReportChild, monday: date, generated_at: datetime):
        super().__init__(orientation="L", unit="mm", format="A4")
        self.child, self.monday, self.generated_at = child, monday, generated_at
        self.set_margins(8, 8, 8)
        self.set_auto_page_break(False)
        self.day_w = (self.epw - LABEL_W) / 5

    def header(self):
        self.set_font("Helvetica", "B", 11)
        self.cell(0, 6, _latin1("FUNDACIÓN ESTANCIAS INFANTILES SONRISAS DE CRISTAL"), align="C",
                  new_x="LMARGIN", new_y="NEXT")
        self.set_font("Helvetica", "", 8)
        friday = self.monday + timedelta(days=4)
        year, week, _ = self.monday.isocalendar()
        self.cell(0, 4.5, _latin1(f"Reporte semanal - Semana {week} de {year} "
                                  f"({self.monday:%d/%m/%Y} al {friday:%d/%m/%Y})"),
                  align="C", new_x="LMARGIN", new_y="NEXT")
        self.ln(1)

    def footer(self):
        self.set_y(-8)
        self.set_font("Helvetica", "I", 7)
        self.cell(0, 4, _latin1(f"Generado el {self.generated_at:%d/%m/%Y %H:%M}"), align="L")
        self.set_x(self.l_margin)
        self.cell(0, 4, _latin1(f"Página {self.page_no()}/{{nb}}"), align="R")

    @property
    def bottom(self) -> float:
        return self.h - 10

    def band(self, title: str, dark: bool = True):
        if self.get_y() + 6 > self.bottom:
            self.add_page()
            self.days_row()
        self.set_fill_color(*(DARK if dark else LIGHT))
        self.set_text_color(*((255, 255, 255) if dark else (0, 0, 0)))
        self.set_font("Helvetica", "B", 8 if dark else 7.5)
        self.cell(0, 5 if dark else 4.2, _latin1(title), border=1, fill=True, align="L" if dark else "C",
                  new_x="LMARGIN", new_y="NEXT")
        self.set_text_color(0, 0, 0)

    def general_data(self):
        self.band("I. Datos Generales")
        self.set_font("Helvetica", "", 8)
        group = self.child.group_type
        group_text = f"{group} ({GROUPS.get(group, group)})" if group else "-"
        salon = f" - Salón: {self.child.salon}" if self.child.salon else ""
        widths = (self.epw * 0.38, self.epw * 0.40, self.epw * 0.22)
        texts = (f"Estancia Infantil: {self.child.estancia}", f"Nombre completo de la niña o niño: {self.child.full_name}",
                 f"Grupo: {group_text}{salon}")
        for w, t in zip(widths, texts):
            self.cell(w, 6, _latin1(t), border=1)
        self.ln(6)

    def days_row(self):
        self.set_font("Helvetica", "B", 7.5)
        self.set_fill_color(*LIGHT)
        self.cell(LABEL_W, 5, "", border=1, fill=True)
        for i, name in enumerate(DAYS):
            day = self.monday + timedelta(days=i)
            self.cell(self.day_w, 5, _latin1(f"{name} {day:%d/%m/%Y}"), border=1, fill=True, align="C")
        self.ln(5)

    def row(self, label: str, values: list[str]):
        texts = [_latin1(label)] + [_latin1(v) for v in values]
        widths = [LABEL_W] + [self.day_w] * 5
        self.set_font("Helvetica", "", 7)
        lines = max(len(self.multi_cell(w - 1, LINE_H, t, dry_run=True, output="LINES")) for w, t in zip(widths, texts))
        height = lines * LINE_H + 1
        if self.get_y() + height > self.bottom:
            self.add_page()
            self.days_row()
            self.set_font("Helvetica", "", 7)
        x, y = self.l_margin, self.get_y()
        for i, (w, t) in enumerate(zip(widths, texts)):
            if i == 0:
                self.set_fill_color(245, 245, 245)
                self.rect(x, y, w, height, style="DF")
            else:
                self.rect(x, y, w, height)
            self.set_xy(x + 0.5, y + 0.5)
            self.multi_cell(w - 1, LINE_H, t, border=0, align="L")
            x += w
        self.set_xy(self.l_margin, y + height)


def render_weekly_report(child: ReportChild, monday: date, logs: dict[date, DailyLogOut],
                         generated_at: datetime) -> bytes:
    """`logs` maps a weekday to its log; days without one are marked as such in every row."""
    week = [logs.get(monday + timedelta(days=i)) for i in range(5)]
    pdf = _Report(child, monday, generated_at)
    pdf.alias_nb_pages()

    def values(part: str, fn: Callable[[DailyLogOut], str]) -> list[str]:
        return [fn(log) if log is not None and getattr(log, part) is not None
                else ("Sin registro" if log is None else "-") for log in week]

    pdf.add_page()
    pdf.general_data()
    pdf.band("II. Registro de ingreso de la niña o niño a la Estancia Infantil")
    pdf.days_row()
    for label, fn in CHECK_IN_ROWS:
        pdf.row(label, values("check_in", fn))
    pdf.band("IV. Registro de egreso de la niña o niño de la Estancia Infantil")
    pdf.days_row()
    for label, fn in CHECK_OUT_ROWS:
        pdf.row(label, values("check_out", fn))

    pdf.add_page()
    pdf.band("III. Registro de estadía de la niña o niño en la Estancia Infantil")
    pdf.days_row()
    for title, rows in STAY_GROUPS:
        pdf.band(title, dark=False)
        for label, fn in rows:
            pdf.row(label, values("stay", fn))
    pdf.band("Actividades", dark=False)
    pdf.row("Actividades realizadas", [_activities(log) if log is not None else "Sin registro" for log in week])
    return bytes(pdf.output())
