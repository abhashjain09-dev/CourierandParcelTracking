package com.courier.gui;

/**
 * @deprecated Moved to {@link com.courier.view.ShipmentForm} to adhere to MVC architecture.
 */
@Deprecated
public class ShipmentForm extends com.courier.view.ShipmentForm {
    public ShipmentForm() {
        super(new com.courier.controller.ShipmentController(
            new com.courier.dao.ShipmentDAO(),
            new com.courier.dao.StatusLogDAO()
        ), null);
    }
}
