import 'package:flutter/foundation.dart';

/// Aviso compartido entre modulos: "cambiaron los datos de dinero".
///
/// Deudas y movimientos estan cruzados en el backend (cada abono es un
/// gasto en la categoria Deudas). Como InicioPantalla usa IndexedStack,
/// cada pestana conserva su estado y no se recarga sola; con este aviso,
/// cuando una pestana registra o borra algo, la otra se entera y recarga.
///
/// Es un ValueNotifier simple: el valor solo sube para disparar el aviso.
class CambiosFinancieros {
  CambiosFinancieros._();

  static final ValueNotifier<int> _version = ValueNotifier<int>(0);

  static Listenable get escuchar => _version;

  static void notificar() => _version.value++;
}
