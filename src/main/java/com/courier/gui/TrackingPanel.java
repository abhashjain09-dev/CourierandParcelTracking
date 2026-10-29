package com.courier.gui;

/**
 * @deprecated Moved to {@link com.courier.view.TrackingPanel} to adhere to MVC architecture.
 */
@Deprecated
public class TrackingPanel extends com.courier.view.TrackingPanel {
    public TrackingPanel() {
        super(new com.courier.controller.ShipmentController(
            new com.courier.dao.ShipmentDAO(),
            new com.courier.dao.StatusLogDAO()
        ), null);
    }
}
