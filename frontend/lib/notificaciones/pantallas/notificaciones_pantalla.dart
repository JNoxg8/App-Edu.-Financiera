import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/api_exception.dart';
import '../modelo/notificacion.dart';
import '../servicio/notificacion_servicio.dart';
import '../../comun/menu_lateral.dart';

class NotificacionesPantalla extends StatefulWidget {
  final NotificacionServicio? servicio;

  const NotificacionesPantalla({super.key, this.servicio});

  @override
  State<NotificacionesPantalla> createState() =>
      _NotificacionesPantallaState();
}

class _NotificacionesPantallaState extends State<NotificacionesPantalla> {
  late final NotificacionServicio _servicio =
      widget.servicio ?? NotificacionServicio();

  List<Notificacion> _notificaciones = [];
  bool _cargando = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargarNotificaciones();
  }

  Future<void> _cargarNotificaciones() async {
    setState(() {
      _cargando = true;
      _error = null;
    });

    try {
      final notificaciones = await _servicio.listar();

      if (!mounted) return;

      setState(() {
        _notificaciones = notificaciones;
        _cargando = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString();
        _cargando = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'No se pudo conectar con el servidor.';
        _cargando = false;
      });
    }
  }

  Future<void> _marcarComoLeida(Notificacion notificacion) async {
    if (notificacion.leida) return;

    try {
      final actualizada =
          await _servicio.marcarComoLeida(notificacion.id);

      if (!mounted) return;

      setState(() {
        final indice = _notificaciones.indexWhere(
          (item) => item.id == actualizada.id,
        );

        if (indice != -1) {
          _notificaciones[indice] = actualizada;
        }
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('No se pudo actualizar la notificación.'),
        ),
      );
    }
  }

  Widget _tarjetaNotificacion(Notificacion notificacion) {
    final tema = Theme.of(context);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: notificacion.leida
          ? null
          : tema.colorScheme.primaryContainer.withValues(alpha: 0.35),
      child: ListTile(
        contentPadding: const EdgeInsets.all(16),
        leading: CircleAvatar(
          backgroundColor: tema.colorScheme.primaryContainer,
          child: Icon(
            notificacion.leida
                ? Icons.notifications_none
                : Icons.notifications_active,
            color: tema.colorScheme.primary,
          ),
        ),
        title: Text(
          notificacion.titulo,
          style: tema.textTheme.titleMedium?.copyWith(
            fontWeight: notificacion.leida
                ? FontWeight.normal
                : FontWeight.bold,
          ),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(notificacion.mensaje),
              const SizedBox(height: 8),
              Text(
                DateFormat('dd/MM/yyyy - hh:mm a', 'es_CO')
                    .format(notificacion.fechaCreacion),
                style: tema.textTheme.bodySmall,
              ),
              if (!notificacion.leida) ...[
                const SizedBox(height: 8),
                TextButton.icon(
                  onPressed: () => _marcarComoLeida(notificacion),
                  icon: const Icon(Icons.done),
                  label: const Text('Marcar como leída'),
                ),
              ],
              if (notificacion.leida)
                const Padding(
                  padding: EdgeInsets.only(top: 8),
                  child: Text('Leída'),
                ),
            ],
          ),
        ),
        isThreeLine: true,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: const BotonMenu(),
        title: const Text('Notificaciones'),
        actions: [
          IconButton(
            onPressed: _cargando ? null : _cargarNotificaciones,
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar notificaciones',
          ),
        ],
      ),
      body: SafeArea(
        child: _cargando
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.error_outline, size: 42),
                          const SizedBox(height: 12),
                          Text(_error!, textAlign: TextAlign.center),
                          const SizedBox(height: 12),
                          FilledButton.icon(
                            onPressed: _cargarNotificaciones,
                            icon: const Icon(Icons.refresh),
                            label: const Text('Reintentar'),
                          ),
                        ],
                      ),
                    ),
                  )
                : _notificaciones.isEmpty
                    ? const Center(
                        child: Padding(
                          padding: EdgeInsets.all(24),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.notifications_none, size: 52),
                              SizedBox(height: 12),
                              Text(
                                'No tienes notificaciones todavía.',
                                textAlign: TextAlign.center,
                              ),
                            ],
                          ),
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: _cargarNotificaciones,
                        child: ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _notificaciones.length,
                          itemBuilder: (context, index) =>
                              _tarjetaNotificacion(_notificaciones[index]),
                        ),
                      ),
      ),
    );
  }
}
