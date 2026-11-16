import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../comun/api_exception.dart';
import '../../config/api_config.dart';
import '../modelo/movimiento.dart';

// Las pantallas de movimientos importan ApiException desde aqui,
// igual que las de deudas lo hacen desde deuda_servicio.dart.
export '../../comun/api_exception.dart' show ApiException;

/// Comunicacion con /movimientos y /categorias (HU009).
/// Las pantallas nunca llaman a http directamente.
class MovimientoServicio {
  static Duration get _timeout => ApiConfig.timeout;

  /// Se puede inyectar para las pruebas (MockClient).
  final http.Client _cliente;

  MovimientoServicio({http.Client? cliente})
      : _cliente = cliente ?? http.Client();

  final String _base = '${ApiConfig.baseUrl}/movimientos';
  final String _baseCategorias = '${ApiConfig.baseUrl}/categorias';

  Map<String, String> get _headers => {'Content-Type': 'application/json'};

  /// El backend espera fechas ISO-8601 sin hora: 2026-09-27
  static String _fecha(DateTime f) => f.toIso8601String().split('T').first;

  dynamic _json(http.Response res) => jsonDecode(utf8.decode(res.bodyBytes));

  // ------------------------------------------------------------------
  // Movimientos
  // ------------------------------------------------------------------

  /// Todos los filtros son opcionales y se pueden combinar.
  Future<List<Movimiento>> listar({
    TipoMovimiento? tipo,
    int? categoriaId,
    String? buscar,
    DateTime? desde,
    DateTime? hasta,
    OrdenMovimientos orden = OrdenMovimientos.fecha,
    bool ascendente = false,
  }) async {
    final parametros = <String, String>{
      if (tipo != null) 'tipo': tipo.codigo,
      if (categoriaId != null) 'categoriaId': '$categoriaId',
      if (buscar != null && buscar.trim().isNotEmpty) 'buscar': buscar.trim(),
      if (desde != null) 'desde': _fecha(desde),
      if (hasta != null) 'hasta': _fecha(hasta),
      'ordenarPor': orden.codigo,
      'direccion': ascendente ? 'ASC' : 'DESC',
    };

    final res = await _cliente
        .get(Uri.parse(_base).replace(queryParameters: parametros))
        .timeout(_timeout);
    verificarRespuesta(res);

    return (_json(res) as List<dynamic>)
        .map((m) => Movimiento.desdeJson(m as Map<String, dynamic>))
        .toList();
  }

  Future<ResumenMovimientos> resumen({DateTime? desde, DateTime? hasta}) async {
    final parametros = <String, String>{
      if (desde != null) 'desde': _fecha(desde),
      if (hasta != null) 'hasta': _fecha(hasta),
    };
    final res = await _cliente
        .get(Uri.parse('$_base/resumen').replace(queryParameters: parametros))
        .timeout(_timeout);
    verificarRespuesta(res);
    return ResumenMovimientos.desdeJson(_json(res) as Map<String, dynamic>);
  }

  /// HU009: registrar un ingreso o gasto.
  /// deudaId solo se envia si la categoria es "Deudas": el gasto se
  /// registra como abono y baja el saldo de esa deuda.
  Future<Movimiento> crear({
    required TipoMovimiento tipo,
    required double monto,
    required DateTime fecha,
    String? descripcion,
    required int categoriaId,
    int? deudaId,
  }) async {
    final res = await _cliente
        .post(
          Uri.parse(_base),
          headers: _headers,
          body: jsonEncode({
            'tipo': tipo.codigo,
            'monto': monto,
            'fecha': _fecha(fecha),
            'descripcion': descripcion,
            'categoriaId': categoriaId,
            'deudaId': deudaId,
          }),
        )
        .timeout(_timeout);
    verificarRespuesta(res);
    return Movimiento.desdeJson(_json(res) as Map<String, dynamic>);
  }

  Future<Movimiento> actualizar({
    required int id,
    required TipoMovimiento tipo,
    required double monto,
    required DateTime fecha,
    String? descripcion,
    required int categoriaId,
  }) async {
    final res = await _cliente
        .put(
          Uri.parse('$_base/$id'),
          headers: _headers,
          body: jsonEncode({
            'tipo': tipo.codigo,
            'monto': monto,
            'fecha': _fecha(fecha),
            'descripcion': descripcion,
            'categoriaId': categoriaId,
          }),
        )
        .timeout(_timeout);
    verificarRespuesta(res);
    return Movimiento.desdeJson(_json(res) as Map<String, dynamic>);
  }

  Future<void> eliminar(int id) async {
    final res =
        await _cliente.delete(Uri.parse('$_base/$id')).timeout(_timeout);
    verificarRespuesta(res);
  }

  // ------------------------------------------------------------------
  // Categorias
  // ------------------------------------------------------------------

  /// Predefinidas primero y despues las personalizadas del usuario.
  Future<List<Categoria>> listarCategorias({TipoMovimiento? tipo}) async {
    final uri = Uri.parse(_baseCategorias).replace(
      queryParameters: tipo == null ? null : {'tipo': tipo.codigo},
    );
    final res = await _cliente.get(uri).timeout(_timeout);
    verificarRespuesta(res);
    return (_json(res) as List<dynamic>)
        .map((c) => Categoria.desdeJson(c as Map<String, dynamic>))
        .toList();
  }

  Future<Categoria> crearCategoria({
    required String nombre,
    required TipoMovimiento tipo,
  }) async {
    final res = await _cliente
        .post(
          Uri.parse(_baseCategorias),
          headers: _headers,
          body: jsonEncode({'nombre': nombre, 'tipo': tipo.codigo}),
        )
        .timeout(_timeout);
    verificarRespuesta(res);
    return Categoria.desdeJson(_json(res) as Map<String, dynamic>);
  }

  Future<Categoria> renombrarCategoria({
    required int id,
    required String nombre,
  }) async {
    final res = await _cliente
        .put(
          Uri.parse('$_baseCategorias/$id'),
          headers: _headers,
          body: jsonEncode({'nombre': nombre}),
        )
        .timeout(_timeout);
    verificarRespuesta(res);
    return Categoria.desdeJson(_json(res) as Map<String, dynamic>);
  }

  Future<void> eliminarCategoria(int id) async {
    final res = await _cliente
        .delete(Uri.parse('$_baseCategorias/$id'))
        .timeout(_timeout);
    verificarRespuesta(res);
  }
}
