package org.augustoocc.controller;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import org.augustoocc.domain.Customer;
import org.augustoocc.reactiveStreams.ReactiveCm;
import org.augustoocc.repository.CustomerRepo;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/api/v1/customer")
@Slf4j
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerAPI {


    @Inject
    CustomerRepo customerRepo;

    @Inject
    ReactiveCm reactiveCm;

    //PostConstruct significa que despues de construir esta clase lo primero que se ejecuta es el codigo
    //que tiene en su metodo correspondiente
    //webclient crea un cliente web reactivo para poder hacer la interaccion
    //Dentro del metodo create se pone la instancia de vertx,  y luego ponemos el web client options, que n
    // que nos permite dar opciones al cliente. En este caso le pusimos host, puerto y que el
    //ssl sea falso. Ademas de confiar en todas las peticiones entrantes
    //Si tuviera mas de un microservicio deberia configurar en vez del localhost, la direccion ip mas el puerto
    // y el certificado

    @GET
    @Blocking
    public List<Customer> list() {
        log.info("Request received - listing objects");
        return customerRepo.listCustomer();

    }

    @POST
    @Blocking
    public Response postCustomer(Customer customer) {
        log.info("Request received - putting object in db");
        customerRepo.createCustomer(customer);
        return Response.ok().build();
    }

    @DELETE
    @Path("delete/{id}")
    @Blocking
    public Response deleteCustomer (@PathParam("id") Long id) {
        log.info("Deleting object with id: ", id);
        customerRepo.deleteCustomer(id);
        return Response.ok().build();
    }

    @PUT
    @Blocking
    public Response putCustomer(Customer customer) {
        log.info("Merging object with id: ", customer.getId());
        customerRepo.putObject(customer);
        return Response.ok().build();
    }


    @GET
    @Path("/id/{id}")
    @Blocking
    public Customer getCustumer(@PathParam("id") Long id) {
        log.info("Request received - getting customer");
        return customerRepo.getCustomer(id);
    }

    //Aca combino los unis para hacer un streams de datos y ahi llamo a los streams de
    //la clase reacttiva que hace el llamado al otro microservicio
    //El combinedwith nos dice que reglas vamos a ponerle a la combinacion.
    //Aca queremos traer los atributos del producto y settearselos a los atributos del customer
    //Esto se hara bloqueante, porque no tenemos base de datos
    //por eso hay que bloquear el hilo
    @GET
    @Path("{id}/customer-products")
    @Blocking
    public Uni<Customer> getProductById(@PathParam("id") Long id) {
       return Uni.combine().all().unis(reactiveCm.getReactiveCustomerStream(id), reactiveCm.listReactiveProducts())
                .combinedWith((customer, listOfProd) -> {
                    customer.getProducts().forEach(productCustomer -> {
                        listOfProd.forEach(originalProduct -> {
                            if(productCustomer.getProduct().equals(originalProduct.getId())) {
                                productCustomer.setName(originalProduct.getName());
                                productCustomer.setDescription(originalProduct.getDescription());

                            }
                        });
                    });
                    return customer;
                });

    }
}