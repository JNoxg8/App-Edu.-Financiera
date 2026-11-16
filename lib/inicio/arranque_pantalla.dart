import 'dart:async';

import 'package:flutter/material.dart';

import '../config/api_config.dart';
import '../perfil/modelo/perfil.dart';
import '../perfil/pantallas/crear_perfil_pantalla.dart';
import '../perfil/servicio/perfil_servicio.dart';
import 'inicio_pantalla.dart';

/// Primera pantalla de la app. Decide a donde ir segun el perfil:
///
///   GET /perfil -> 200  -> pantalla principal
///               -> 404  -> formulario de creacion (HU001: solo la primera vez)
///               -> error de conexion -> mensaje con boton de reintentar
class ArranquePantalla extends StatefulWidget {
  /// Se puede inyectar para las pruebas; en la app se usa el real.
  final PerfilServicio? servicio;

  const ArranquePantalla({super.key, this.servicio});

  @override
  State<ArranquePantalla> createState() => _ArranquePantallaState();
}

class _ArranquePantallaState extends State<ArranquePantalla> {
  late final PerfilServicio _servicio = widget.servicio ?? PerfilServicio();

  bool _cargando = true;
  bool _error = false;
  Perfil? _perfil;

  /// true si la primera respuesta tarda: el servidor de Render se esta
  /// encendiendo (plan gratuito) y conviene explicarlo.
  bool _lento = false;
  Timer? _temporizador;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  @override
  void dispose() {
    _temporizador?.cancel();
    super.dispose();
  }

  Future<void> _cargar() async {
    setState(() {
      _cargando = true;
      _error = false;
      _lento = false;
    });
    _temporizador?.cancel();
    _temporizador = Timer(const Duration(seconds: 5), () {
      if (mounted && _cargando) setState(() => _lento = true);
    });
    try {
      final perfil = await _servicio.obtener();
      if (!mounted) return;
      setState(() {
        _perfil = perfil;
        _cargando = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = true;
        _cargando = false;
      });
    } finally {
      _temporizador?.cancel();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_cargando) {
      return Scaffold(
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const CircularProgressIndicator(),
                const SizedBox(height: 20),
                Text(
                  _lento && ApiConfig.usaRender
                      ? 'Activando el servidor...\nLa primera conexión después de '
                          'un rato sin uso puede tardar hasta un minuto.'
                      : 'Conectando con el servidor...',
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        ),
      );
    }

    if (_error) {
      return Scaffold(
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.cloud_off, size: 64),
                const SizedBox(height: 12),
                const Text('No se pudo conectar con el servidor',
                    textAlign: TextAlign.center),
                const SizedBox(height: 16),
                FilledButton.icon(
                  onPressed: _cargar,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Reintentar'),
                ),
              ],
            ),
          ),
        ),
      );
    }

    final perfil = _perfil;
    if (perfil == null) {
      // No se usa Navigator: al crear el perfil solo cambia el estado, asi
      // el formulario desaparece y no se puede volver a el con "atras".
      return CrearPerfilPantalla(
        servicio: _servicio,
        onCreado: (nuevo) => setState(() => _perfil = nuevo),
      );
    }

    return InicioPantalla(perfil: perfil);
  }
}
