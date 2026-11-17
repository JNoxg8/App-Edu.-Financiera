import 'package:flutter/material.dart';

import '../../comun/tema.dart';
import '../servicio/autenticacion_servicio.dart';

/// Pantalla de acceso: iniciar sesion o crear una cuenta.
///
/// Cada persona tiene su cuenta, y la app solo le muestra sus propios
/// datos (deudas, metas, movimientos...). Al terminar llama a
/// [onAutenticado] y la pantalla de arranque continua con el perfil.
class AccesoPantalla extends StatefulWidget {
  final VoidCallback onAutenticado;

  /// Se puede inyectar para las pruebas.
  final AutenticacionServicio? servicio;

  const AccesoPantalla({super.key, required this.onAutenticado, this.servicio});

  @override
  State<AccesoPantalla> createState() => _AccesoPantallaState();
}

class _AccesoPantallaState extends State<AccesoPantalla> {
  late final AutenticacionServicio _servicio =
      widget.servicio ?? AutenticacionServicio();

  final _formulario = GlobalKey<FormState>();
  final _correo = TextEditingController();
  final _contrasena = TextEditingController();
  final _confirmacion = TextEditingController();

  bool _registro = false;
  bool _ocultar = true;
  bool _enviando = false;
  String? _error;

  @override
  void dispose() {
    _correo.dispose();
    _contrasena.dispose();
    _confirmacion.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    setState(() => _error = null);
    if (!_formulario.currentState!.validate()) return;

    setState(() => _enviando = true);
    try {
      if (_registro) {
        await _servicio.registrar(_correo.text, _contrasena.text);
      } else {
        await _servicio.iniciarSesion(_correo.text, _contrasena.text);
      }
      if (!mounted) return;
      widget.onAutenticado();
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _error = e.mensaje);
    } catch (_) {
      if (!mounted) return;
      setState(() => _error = 'No se pudo conectar con el servidor. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  void _cambiarModo() {
    setState(() {
      _registro = !_registro;
      _error = null;
      _confirmacion.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [ColoresZenfi.azulMarino, ColoresZenfi.azulProfundo],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 420),
                child: Column(
                  children: [
                    const CircleAvatar(
                      radius: 34,
                      backgroundColor: ColoresZenfi.petroleo,
                      child: Icon(Icons.savings, size: 36, color: Colors.white),
                    ),
                    const SizedBox(height: 12),
                    Text('Zenfi',
                        style: tema.textTheme.headlineMedium?.copyWith(
                            color: Colors.white, fontWeight: FontWeight.bold)),
                    Text('Tu educación financiera, en orden',
                        style: tema.textTheme.bodyMedium
                            ?.copyWith(color: const Color(0xFFC9D6E6))),
                    const SizedBox(height: 24),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(20),
                        child: Form(
                          key: _formulario,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Text(
                                _registro ? 'Crear cuenta' : 'Iniciar sesión',
                                style: tema.textTheme.titleLarge
                                    ?.copyWith(fontWeight: FontWeight.w600),
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _correo,
                                keyboardType: TextInputType.emailAddress,
                                autofillHints: const [AutofillHints.email],
                                decoration: const InputDecoration(
                                  labelText: 'Correo',
                                  prefixIcon: Icon(Icons.mail_outline),
                                ),
                                validator: (v) {
                                  final valor = v?.trim() ?? '';
                                  if (valor.isEmpty) return 'Escribe tu correo';
                                  if (!valor.contains('@') || !valor.contains('.')) {
                                    return 'El correo no tiene un formato válido';
                                  }
                                  return null;
                                },
                              ),
                              const SizedBox(height: 12),
                              TextFormField(
                                controller: _contrasena,
                                obscureText: _ocultar,
                                autofillHints: [
                                  _registro
                                      ? AutofillHints.newPassword
                                      : AutofillHints.password
                                ],
                                decoration: InputDecoration(
                                  labelText: 'Contraseña',
                                  prefixIcon: const Icon(Icons.lock_outline),
                                  helperText: _registro ? 'Mínimo 8 caracteres' : null,
                                  suffixIcon: IconButton(
                                    tooltip: _ocultar ? 'Mostrar' : 'Ocultar',
                                    icon: Icon(_ocultar
                                        ? Icons.visibility_outlined
                                        : Icons.visibility_off_outlined),
                                    onPressed: () =>
                                        setState(() => _ocultar = !_ocultar),
                                  ),
                                ),
                                validator: (v) {
                                  if (v == null || v.isEmpty) return 'Escribe tu contraseña';
                                  if (_registro && v.length < 8) {
                                    return 'Debe tener al menos 8 caracteres';
                                  }
                                  return null;
                                },
                                onFieldSubmitted: (_) => _registro ? null : _enviar(),
                              ),
                              if (_registro) ...[
                                const SizedBox(height: 12),
                                TextFormField(
                                  controller: _confirmacion,
                                  obscureText: _ocultar,
                                  decoration: const InputDecoration(
                                    labelText: 'Confirmar contraseña',
                                    prefixIcon: Icon(Icons.lock_outline),
                                  ),
                                  validator: (v) => v != _contrasena.text
                                      ? 'Las contraseñas no coinciden'
                                      : null,
                                  onFieldSubmitted: (_) => _enviar(),
                                ),
                              ],
                              if (_error != null) ...[
                                const SizedBox(height: 12),
                                Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: ColoresZenfi.negativoFondo,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Row(
                                    children: [
                                      const Icon(Icons.error_outline,
                                          color: ColoresZenfi.negativo, size: 20),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(_error!,
                                            style: const TextStyle(
                                                color: ColoresZenfi.negativoTexto)),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                              const SizedBox(height: 20),
                              FilledButton(
                                onPressed: _enviando ? null : _enviar,
                                style: FilledButton.styleFrom(
                                    minimumSize: const Size.fromHeight(48)),
                                child: _enviando
                                    ? const SizedBox(
                                        width: 22,
                                        height: 22,
                                        child: CircularProgressIndicator(
                                            strokeWidth: 2.5, color: Colors.white),
                                      )
                                    : Text(_registro ? 'Crear cuenta' : 'Entrar'),
                              ),
                              const SizedBox(height: 8),
                              TextButton(
                                onPressed: _enviando ? null : _cambiarModo,
                                child: Text(_registro
                                    ? '¿Ya tienes cuenta? Inicia sesión'
                                    : '¿No tienes cuenta? Crea una'),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
