package com.moneybags.branch_employee_service.dto.request;

import jakarta.validation.constraints.Pattern;

public class UpdateBranchRequest {

    private String name;
    private String address;
    private String city;
    private String state;

    @Pattern(regexp = "\\d{6}", message = "pincode must be 6 digits")
    private String pincode;

    // getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
}
