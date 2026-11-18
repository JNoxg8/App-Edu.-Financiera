"""Genera el icono de Zenfi: anillo de progreso + letra Z geometrica.

Uso (desde frontend/): python tool/generar_icono.py assets/icono
      y luego: dart run flutter_launcher_icons
Crea:
  zenfi_icono.png        1024x1024 con fondo (Android antiguo, web, favicon)
  zenfi_primer_plano.png 1024x1024 transparente (icono adaptativo de Android)
"""
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

AZUL_MARINO = (0x1B, 0x36, 0x5D, 255)
PISTA = (0x2C, 0x4C, 0x78, 255)       # anillo de fondo, un poco mas claro que el marino
PETROLEO = (0x3F, 0xB0, 0xA8, 255)    # avance del anillo (petroleo claro, buen contraste)
BLANCO = (0xFF, 0xFF, 0xFF, 255)

ESCALA = 4            # se dibuja a 4x y se reduce: bordes suaves (antialiasing)
LADO = 1024


def dibujar(escala_contenido: float, con_fondo: bool) -> Image.Image:
    n = LADO * ESCALA
    img = Image.new("RGBA", (n, n), AZUL_MARINO if con_fondo else (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = n / 2

    # Anillo: radio externo y grosor relativos al lado
    r = n * 0.36 * escala_contenido
    grosor = n * 0.085 * escala_contenido
    caja = [c - r, c - r, c + r, c + r]
    d.arc(caja, 0, 360, fill=PISTA, width=int(grosor))
    # 75 % de avance empezando arriba (-90 grados), en sentido horario
    d.arc(caja, -90, 180, fill=PETROLEO, width=int(grosor))
    # Extremos redondeados del avance
    rm = r - grosor / 2
    for ang in (-90, 180):
        x = c + rm * math.cos(math.radians(ang))
        y = c + rm * math.sin(math.radians(ang))
        d.ellipse([x - grosor / 2, y - grosor / 2, x + grosor / 2, y + grosor / 2], fill=PETROLEO)

    # Z geometrica (poligono, sin depender de una fuente)
    a = n * 0.30 * escala_contenido   # ancho de la Z
    h = n * 0.30 * escala_contenido   # alto de la Z
    t = n * 0.065 * escala_contenido  # grosor del trazo
    x0, y0 = c - a / 2, c - h / 2
    x1, y1 = c + a / 2, c + h / 2
    z = [
        (x0, y0), (x1, y0),           # barra superior
        (x1, y0 + t),
        (x0 + t * 1.35, y1 - t),      # diagonal
        (x1, y1 - t), (x1, y1),       # barra inferior
        (x0, y1),
        (x0, y1 - t),
        (x1 - t * 1.35, y0 + t),
        (x0, y0 + t),
    ]
    d.polygon(z, fill=BLANCO)

    return img.resize((LADO, LADO), Image.LANCZOS)


def main() -> None:
    destino = Path(sys.argv[1])
    destino.mkdir(parents=True, exist_ok=True)
    dibujar(1.0, True).save(destino / "zenfi_icono.png")
    # flutter_launcher_icons ya deja un margen (inset) del 16 % alrededor del
    # primer plano, asi que la figura queda dentro de la zona segura de Android.
    dibujar(1.0, False).save(destino / "zenfi_primer_plano.png")
    print("Iconos generados en", destino)


if __name__ == "__main__":
    main()
