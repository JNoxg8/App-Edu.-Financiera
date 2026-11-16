package edu.ucentral.zenfi.panel.dominio.puerto;

import edu.ucentral.zenfi.panel.dominio.modelo.ResumenDeuda;

import java.util.List;

/** Puerto de salida: las deudas que todavia tienen saldo (HU007). */
public interface DeudasPanelPuerto {

    List<ResumenDeuda> deudasPendientes(Long usuarioId);
}
