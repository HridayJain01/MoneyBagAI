package com.moneybags.branch_employee_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateBranchRequest {

    @NotBlank(message = "branchCode is required")
    private String branchCode;

    @NotBlank(message = "name is required")
    private String name;

    private String address;
    private String city;
    private String state;

    @Pattern(regexp = "\\d{6}", message = "pincode must be 6 digits")
    private String pincode;

    @NotBlank(message = "ifscCode is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "ifscCode format is invalid")
    private String ifscCode;

    // getters and setters
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
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
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
}
