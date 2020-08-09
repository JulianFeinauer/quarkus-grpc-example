package com.example;

import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import org.pragmaticindustries.dc.ListRequest;
import org.pragmaticindustries.dc.ListResponse;
import org.pragmaticindustries.dc.ModuleIdentifier;
import org.pragmaticindustries.dc.ModuleRegistryGrpc;
import org.pragmaticindustries.dc.RegistrationRequest;
import org.pragmaticindustries.dc.RegistrationResponse;
import org.pragmaticindustries.dc.UnregistrationRequest;
import org.pragmaticindustries.dc.UnregistrationResponse;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Iterator;

@Path("/hello")
public class ExampleResource {

    @Inject
    @GrpcService("module-registry")
    ModuleRegistryGrpc.ModuleRegistryBlockingStub moduleRegistry;

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_PLAIN)
    public String register() {
        try {
            final RegistrationResponse response = moduleRegistry.register(
                RegistrationRequest.newBuilder()
                    .setId(
                        ModuleIdentifier.newBuilder()
                            .setCompany("pragmaticminds")
                            .setApp("dummy-app")
                            .setVersion("1.0.0")
                            .build()
                    )
                    .build()
            );
            return response.toString();
        } catch (StatusRuntimeException e) {
            return "GRPC Exception!" +  e.getStatus();
        }
    }

    @GET
    @Path("/unregister")
    @Produces(MediaType.TEXT_PLAIN)
    public String unregister() {
        try {
            final UnregistrationResponse response = moduleRegistry.unregister(
                UnregistrationRequest.newBuilder()
                    .setId(
                        ModuleIdentifier.newBuilder()
                            .setCompany("pragmaticminds")
                            .setApp("dummy-app")
                            .setVersion("1.0.0")
                            .build()
                    )
                    .build()
            );
            return "success: " + response.getSuccess();
        } catch (StatusRuntimeException e) {
            return "GRPC Exception!" +  e.getStatus();
        }
    }

    @GET
    @Path("/list")
    @Produces(MediaType.TEXT_PLAIN)
    public String list() {
        try {
            final ListResponse response = moduleRegistry.list(ListRequest.newBuilder().build());

            return response.getModulesList().toString();
        } catch (StatusRuntimeException e) {
            return "GRPC Exception!" +  e.getStatus();
        }
    }

    @GET
    @Path("/lista")
    @Produces(MediaType.TEXT_PLAIN)
    public String listAsync() {
        try {
            final Iterator<ModuleIdentifier> iterator = moduleRegistry.asyncList(ListRequest.newBuilder().build());

            final StringBuilder sb = new StringBuilder();
            iterator.forEachRemaining(mi -> sb.append(mi.toString() + "\n"));

            return sb.toString();
        } catch (StatusRuntimeException e) {
            return "GRPC Exception!" +  e.getStatus();
        }
    }
}