package com.inventario.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase que representa un movimiento de stock en el inventario
 */
public class MovimientoStock {
    @JsonProperty("id_movimiento")
    private int idMovimiento;

    @JsonProperty("id_producto")
    private int idProducto;

    @JsonProperty("tipo_movimiento")
    private String tipoMovimiento; // ENTRADA o SALIDA

    private int cantidad;

    @JsonProperty("stock_anterior")
    private int stockAnterior;

    @JsonProperty("stock_nuevo")
    private int stockNuevo;

    private String motivo;

    @JsonProperty("fecha_movimiento")
    private LocalDateTime fechaMovimiento;

    private String usuario;

    // Constructor vacío
    public MovimientoStock() {}

    // Constructor completo
    public MovimientoStock(int idProducto, String tipoMovimiento, int cantidad,
                          int stockAnterior, int stockNuevo, String motivo, String usuario) {
        this.idProducto = idProducto;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.motivo = motivo;
        this.usuario = usuario;
    }

    // Constructor sin ID (para inserción)
    public MovimientoStock(int idProducto, String tipoMovimiento, int cantidad, String motivo, String usuario) {
        this.idProducto = idProducto;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.usuario = usuario != null && !usuario.trim().isEmpty() ? usuario : "sistema";
    }

    // Getters y Setters
    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento != null ? tipoMovimiento.trim().toUpperCase() : null;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getStockAnterior() {
        return stockAnterior;
    }

    public void setStockAnterior(int stockAnterior) {
        this.stockAnterior = stockAnterior;
    }

    public int getStockNuevo() {
        return stockNuevo;
    }

    public void setStockNuevo(int stockNuevo) {
        this.stockNuevo = stockNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo != null ? motivo.trim() : null;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario != null && !usuario.trim().isEmpty() ? usuario.trim() : "sistema";
    }

    // Métodos de utilidad
    public boolean isValid() {
        return idProducto > 0 &&
               tipoMovimiento != null && (tipoMovimiento.equals("ENTRADA") || tipoMovimiento.equals("SALIDA")) &&
               cantidad > 0;
    }

    public boolean esEntrada() {
        return "ENTRADA".equals(tipoMovimiento);
    }

    public boolean esSalida() {
        return "SALIDA".equals(tipoMovimiento);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovimientoStock that = (MovimientoStock) o;
        return idMovimiento == that.idMovimiento;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMovimiento);
    }

    @Override
    public String toString() {
        return "MovimientoStock{" +
                "idMovimiento=" + idMovimiento +
                ", idProducto=" + idProducto +
                ", tipoMovimiento='" + tipoMovimiento + '\'' +
                ", cantidad=" + cantidad +
                ", stockAnterior=" + stockAnterior +
                ", stockNuevo=" + stockNuevo +
                ", motivo='" + motivo + '\'' +
                ", fechaMovimiento=" + fechaMovimiento +
                ", usuario='" + usuario + '\'' +
                '}';
    }
}
