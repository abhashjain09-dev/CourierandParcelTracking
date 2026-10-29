package com.courier.model;

public class Person {
    private String name, address, city, state, pincode, phone, email;

    public Person() {}

    public Person(String name, String address, String city, String state,
                  String pincode, String phone, String email) {
        this.name = name; this.address = address; this.city = city;
        this.state = state; this.pincode = pincode;
        this.phone = phone; this.email = email;
    }

    public String getName()    { return name; }
    public String getAddress() { return address; }
    public String getCity()    { return city; }
    public String getState()   { return state; }
    public String getPincode() { return pincode; }
    public String getPhone()   { return phone; }
    public String getEmail()   { return email; }

    public void setName(String v)    { this.name = v; }
    public void setAddress(String v) { this.address = v; }
    public void setCity(String v)    { this.city = v; }
    public void setState(String v)   { this.state = v; }
    public void setPincode(String v) { this.pincode = v; }
    public void setPhone(String v)   { this.phone = v; }
    public void setEmail(String v)   { this.email = v; }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.trim().isEmpty()) sb.append(address.trim());
        if (city != null && !city.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city.trim());
        }
        if (state != null && !state.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state.trim());
        }
        if (pincode != null && !pincode.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(pincode.trim());
        }
        return sb.length() > 0 ? sb.toString() : "N/A";
    }

    @Override
    public String toString() {
        return name + " | " + getFullAddress() + " | Ph: " + phone;
    }
}
