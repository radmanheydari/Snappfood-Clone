package com.snappfood.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Embeddable;

@Embeddable
@Getter
@Setter

public class BankInfo {
    private  String bank_name;
    private  String account_number;
}
