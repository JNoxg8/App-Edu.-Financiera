package edu.ucentral.zenfi.panel.dominio.puerto;

import edu.ucentral.zenfi.panel.dominio.modelo.ResumenMeta;

import java.util.List;

/** Puerto de salida: las metas de ahorro activas (HU004). */
public interface MetasPanelPuerto {

    List<ResumenMeta> metasActivas(Long usuarioId);
}
