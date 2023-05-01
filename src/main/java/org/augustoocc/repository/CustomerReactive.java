package org.augustoocc.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import org.augustoocc.domain.Customer;
import org.augustoocc.exceptions.NotFoundEx;
import org.augustoocc.exceptions.NotWritableEx;
import org.augustoocc.reactiveStreams.CustomerMessage;
import org.augustoocc.validations.Validations;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;


import static javax.ws.rs.core.Response.Status.*;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.NOT_FOUND;

@Slf4j
@ApplicationScoped
public class CustomerReactive {

    @Inject
    NotWritableEx exception;

    @Inject
    CustomerRepository customerRepository;


    @Inject
    NotFoundEx notFoundEx;

    @Inject
    Validations validate;


    @ConsumeEvent("add-customer")
    public Uni<Customer> addCustomer(Customer c) throws NotWritableEx {
        log.info("Adding customer with id: ", c.getNames());
        if (validate.postValidation(c) == true) {
            throw exception.nullValues("Post method 'add-customer', ");
        } else {
            return Panache.withTransaction(c::persist)
                    .replaceWith(c)
                    .onFailure().invoke(i -> exception.panacheFailure("Post method 'add-customer'"));
        }
    }


    @ConsumeEvent("delete-customer")
    public Uni<Response> delete(Long Id) {
        return Panache.withTransaction(() -> Customer.deleteById(Id))
                .map(deleted -> deleted
                        ? Response.ok().status(NO_CONTENT).build()
                        : Response.ok().status(NOT_FOUND).build());
    }

    @ConsumeEvent("update-customer")
    public Uni<Response> updateCustomer(CustomerMessage customer) {
        if (validate.postValidation(customer.getCustomer())) {
            throw exception.nullValues("Put method 'add-customer', ");
        }
        log.info("Merging object with id: ", customer.getId());
        return Panache.withTransaction(() -> Customer.<Customer>findById(customer.getId())
                .onItem().ifNotNull().invoke(entity -> {
                    entity.setNames(customer.getCustomer().getNames());
                    entity.setAccountNumber(customer.getCustomer().getAccountNumber());
                    entity.setCode(customer.getCustomer().getCode());
                }))
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                .onItem().ifNull().continueWith(Response.ok().status(NOT_FOUND)::build);
    }


    @ConsumeEvent("get-by-id")
    public Uni<Customer> getById(Long id) {
        log.info("Request received - getting customer");
        return Panache.withTransaction(() -> customerRepository.findById(id));
    }

}
