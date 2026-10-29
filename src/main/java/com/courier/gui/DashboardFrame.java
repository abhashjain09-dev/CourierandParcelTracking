package com.courier.gui;

/**
 * @deprecated Moved to {@link com.courier.view.DashboardFrame} to adhere to MVC architecture.
 */
@Deprecated
public class DashboardFrame extends com.courier.view.DashboardFrame {
    public DashboardFrame() {
        super(new com.courier.controller.ShipmentController(
            new com.courier.dao.ShipmentDAO(),
            new com.courier.dao.StatusLogDAO()
        ));
    }
}
