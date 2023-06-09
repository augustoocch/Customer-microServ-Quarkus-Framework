package org.augustoocc.data;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import org.augustoocc.repository.CustomerRepository;
import org.augustoocc.domain.Customer;
import org.augustoocc.exceptions.NotWritableEx;
import org.augustoocc.reactiveStreams.CustomerMessage;
import org.augustoocc.validations.Validations;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


@Slf4j
@ApplicationScoped
public class DataAccessObjects {

    @Inject
    NotWritableEx exception;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    Validations validate;

    @Inject
    DateTimeFormatter logtimestamp;


    @ConsumeEvent("add-customer")
    public Uni<Customer> addCustomer(Customer c) throws NotWritableEx {
        log.info("Adding customer in timestamp: ", LocalDateTime.now(ZoneOffset.UTC).format(logtimestamp));
        if (validate.postValidation(c)) {
            throw exception.nullValues("Post method 'add-customer', ");
        } else {
            return Panache.withTransaction(c::persist)
                    .invoke(i -> {log.info("Persisting object in the DB at: {}", LocalDateTime.now(ZoneOffset.UTC).format(logtimestamp));})
                    .replaceWith(c)
                    .onFailure().invoke(i -> exception.panacheFailure("Post method 'add-customer'"));
        }
    }

    //@ConsumeEvent("delete-customer")
    public Uni<Response> deleteCustomer(Long Id) {
        return Panache.withTransaction(() -> Customer.deleteById(Id))
                .invoke(i -> {log.info("Deleting object in the DB at: {}", LocalDateTime.now(ZoneOffset.UTC).format(logtimestamp));})
                .map(deleted -> deleted
                        ? Response.ok().status(200).build()
                        : Response.ok().status(404).build());
    }

    @ConsumeEvent("update-customer")
    @Transactional
    public Uni<Customer> updateCustomer(CustomerMessage customer) {
        if (validate.postValidation(customer.getCustomer())) {
            throw exception.nullValues("Put method 'add-customer', ");
        }
        log.info("Merging object with id: ", customer.getId());
        return Panache.withTransaction(() -> customerRepository.findById(customer.getId())
                        .invoke(i -> {log.info("updating object in the DB at: {}", LocalDateTime.now(ZoneOffset.UTC).format(logtimestamp));})
                        .onItem().ifNotNull().invoke(entity -> {
                            entity.setNames(customer.getCustomer().getNames());
                            entity.setSurname(customer.getCustomer().getSurname());
                            entity.setPhone(customer.getCustomer().getPhone());
                            entity.setAddress(customer.getCustomer().getAddress());
                            entity.setAccountNumber(customer.getCustomer().getAccountNumber());
                            entity.setCode(customer.getCustomer().getCode());
                            entity.setProducts(customer.getCustomer().getProducts());
                        }))
                .replaceWith(customer.getCustomer())
                .onFailure().invoke(i -> exception.panacheFailure("Put method 'add-customer'"));
    }


    @ConsumeEvent("get-by-id")
    public Uni<Customer> getById(Long id) {
        log.info("Request received - getting customer");
        return Panache.withTransaction(() -> customerRepository.findById(id))
                .invoke(i -> {log.info("Recovering object in the DB at: {}", LocalDateTime.now(ZoneOffset.UTC).format(logtimestamp));})
                .onFailure().invoke(res -> log.error("Error recovering customers ", res));
    }
}


