package org.augustoocc.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Customer extends PanacheEntity {

   //Cuando usamos panache entity se quita el id
    //porque panache tiene su propio id.
    private String code;
    private String accountNumber;
    private String names;
    private String surname;
    private String phone;
    private String address;
    @OneToMany(mappedBy = "customer",cascade = {CascadeType.ALL},fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Product> products;

}
