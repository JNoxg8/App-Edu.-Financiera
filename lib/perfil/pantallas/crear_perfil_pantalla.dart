import 'package:flutter/material.dart';

import '../../comun/api_exception.dart';
import '../../comun/moneda.dart';
import '../modelo/perfil.dart';
import '../servicio/perfil_servicio.dart';
import '../../comun/tema.dart';

/// HU001: formulario de creacion de perfil.
///
/// Solo aparece la primera vez: ArranquePantalla lo muestra cuando el
/// backend responde 404 en GET /perfil. Al crear el perfil avisa con
/// [onCreado] y la app pasa a la pantalla principal.
class CrearPerfilPantalla extends StatefulWidget {
  final ValueChanged<Perfil> onCreado;

  /// Se puede inyectar para las pruebas; en la app se usa el real.
  final PerfilServicio? servicio;

  const CrearPerfilPantalla({super.key, required this.onCreado, this.servicio});

  @override
  State<CrearPerfilPantalla> createState() => _CrearPerfilPantallaState();
}

class _CrearPerfilPantallaState extends State<CrearPerfilPantalla> {
  final _formKey = GlobalKey<FormState>();
  final _nombre = TextEditingController();
  late final PerfilServicio _servicio = widget.servicio ?? PerfilServicio();

  Moneda? _moneda;
  NivelConocimiento? _nivel;
  bool _guardando = false;

  @override
  void dispose() {
    _nombre.dispose();
    super.dispose();
  }

  void _mostrarError(String texto) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(texto),
      backgroundColor: ColoresZenfi.negativo,
    ));
  }

  Future<void> _guardar() async {
    // Criterio HU001: si falta un campo obligatorio, se notifica y NO se guarda.
    if (!_formKey.currentState!.validate()) {
      _mostrarError('Completa los campos obligatorios');
      return;
    }

    setState(() => _guardando = true);
    try {
      final perfil = await _servicio.crear(
        nombre: _nombre.text.trim(),
        moneda: _moneda!,
        nivel: _nivel!,
      );
      widget.onCreado(perfil);
    } on ApiException catch (e) {
      if (e.status == 409) {
        // El perfil ya existia (por ejemplo, se creo desde otro dispositivo):
        // no es un error para el usuario, se recupera y se continua.
        await _continuarConPerfilExistente();
        return;
      }
      _mostrarError(e.toString());
    } catch (_) {
      _mostrarError('No se pudo conectar con el servidor');
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  Future<void> _continuarConPerfilExistente() async {
    try {
      final existente = await _servicio.obtener();
      if (existente != null) {
        widget.onCreado(existente);
        return;
      }
    } catch (_) {
      // Si falla, se informa abajo
    }
    _mostrarError('No se pudo recuperar el perfil');
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Form(
          key: _formKey,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          child: ListView(
            padding: const EdgeInsets.all(24),
            children: [
              Icon(Icons.savings_outlined,
                  size: 64, color: tema.colorScheme.primary),
              const SizedBox(height: 12),
              Text('Bienvenido a Zenfi',
                  textAlign: TextAlign.center,
                  style: tema.textTheme.headlineSmall
                      ?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text(
                'Crea tu perfil para personalizar la aplicación '
                'y empezar a aprender desde tu nivel.',
                textAlign: TextAlign.center,
                style: tema.textTheme.bodyMedium,
              ),
              const SizedBox(height: 28),

              // ---------- Nombre ----------
              TextFormField(
                key: const Key('campo_nombre'),
                controller: _nombre,
                textCapitalization: TextCapitalization.words,
                maxLength: 80,
                decoration: const InputDecoration(
                  labelText: 'Nombre',
                  prefixIcon: Icon(Icons.person_outline),
                  border: OutlineInputBorder(),
                ),
                validator: (valor) => (valor == null || valor.trim().isEmpty)
                    ? 'El nombre es obligatorio'
                    : null,
              ),
              const SizedBox(height: 16),

              // ---------- Moneda ----------
              Text('Moneda de preferencia', style: tema.textTheme.titleSmall),
              const SizedBox(height: 4),
              Text(
                'Solo cambia cómo se muestran los montos; no convierte valores.',
                style: tema.textTheme.bodySmall
                    ?.copyWith(color: tema.colorScheme.outline),
              ),
              const SizedBox(height: 8),
              FormField<Moneda>(
                validator: (valor) =>
                    valor == null ? 'Selecciona una moneda' : null,
                builder: (campo) => Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    SegmentedButton<Moneda>(
                      emptySelectionAllowed: true,
                      segments: Moneda.values
                          .map((m) => ButtonSegment<Moneda>(
                                value: m,
                                label: Text(m.codigo),
                              ))
                          .toList(),
                      selected: campo.value == null
                          ? <Moneda>{}
                          : {campo.value!},
                      onSelectionChanged: (seleccion) {
                        final elegida =
                            seleccion.isEmpty ? null : seleccion.first;
                        campo.didChange(elegida);
                        setState(() => _moneda = elegida);
                      },
                    ),
                    if (campo.value != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Text(
                          '${campo.value!.nombre} · Ejemplo: '
                          '${campo.value!.formato.format(1500000)}',
                          style: tema.textTheme.bodySmall,
                        ),
                      ),
                    _ErrorDeCampo(campo.errorText),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // ---------- Nivel ----------
              Text('Nivel de conocimiento financiero',
                  style: tema.textTheme.titleSmall),
              const SizedBox(height: 8),
              FormField<NivelConocimiento>(
                validator: (valor) =>
                    valor == null ? 'Selecciona tu nivel de conocimiento' : null,
                builder: (campo) => Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    for (final nivel in NivelConocimiento.values)
                      Card(
                        color: campo.value == nivel
                            ? tema.colorScheme.primaryContainer
                            : null,
                        child: ListTile(
                          title: Text(nivel.nombre),
                          subtitle: Text(nivel.descripcion),
                          trailing: Icon(
                            campo.value == nivel
                                ? Icons.check_circle
                                : Icons.circle_outlined,
                            color: campo.value == nivel
                                ? tema.colorScheme.primary
                                : null,
                          ),
                          onTap: () {
                            campo.didChange(nivel);
                            setState(() => _nivel = nivel);
                          },
                        ),
                      ),
                    _ErrorDeCampo(campo.errorText),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              FilledButton.icon(
                key: const Key('boton_crear_perfil'),
                onPressed: _guardando ? null : _guardar,
                icon: _guardando
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.check),
                label: const Text('Crear perfil'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Muestra el error de un FormField con el mismo estilo de los TextFormField.
class _ErrorDeCampo extends StatelessWidget {
  final String? texto;

  const _ErrorDeCampo(this.texto);

  @override
  Widget build(BuildContext context) {
    if (texto == null) return const SizedBox.shrink();
    final tema = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: 6, left: 12),
      child: Text(
        texto!,
        style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.error),
      ),
    );
  }
}
