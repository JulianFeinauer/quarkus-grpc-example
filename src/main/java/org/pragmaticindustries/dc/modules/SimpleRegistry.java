package org.pragmaticindustries.dc.modules;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.pragmaticindustries.dc.FindInstancesRequest;
import org.pragmaticindustries.dc.FindInstancesResponse;
import org.pragmaticindustries.dc.ListRequest;
import org.pragmaticindustries.dc.ListResponse;
import org.pragmaticindustries.dc.ModuleIdentifier;
import org.pragmaticindustries.dc.ModuleRegistryGrpc;
import org.pragmaticindustries.dc.RegistrationRequest;
import org.pragmaticindustries.dc.RegistrationResponse;
import org.pragmaticindustries.dc.UnregistrationRequest;
import org.pragmaticindustries.dc.UnregistrationResponse;

import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Simple Module Registry.
 */
@Singleton
public class SimpleRegistry extends ModuleRegistryGrpc.ModuleRegistryImplBase {

    private final Set<ModuleIdentifier> services = ConcurrentHashMap.newKeySet();

    @Override
    public void register(RegistrationRequest request, StreamObserver<RegistrationResponse> responseObserver) {
        if (!request.hasId()) {
            responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException());
            return;
        }
        final ModuleIdentifier id = request.getId();
        // Check if the Module is already registered in a different version
        final Set<ModuleIdentifier> installedInstances = services.stream()
            .filter(mi -> id.getCompany().equals(mi.getCompany()) && id.getApp().equals(mi.getApp()))
            .collect(Collectors.toSet());
        if (installedInstances.isEmpty()) {
            // Register
            this.services.add(id);
            responseObserver.onNext(RegistrationResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.ALREADY_EXISTS.asRuntimeException());
        }
    }

    @Override
    public void unregister(UnregistrationRequest request, StreamObserver<UnregistrationResponse> responseObserver) {
        if (!request.hasId()) {
            responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException());
            return;
        }
        final ModuleIdentifier id = request.getId();
        responseObserver.onNext(UnregistrationResponse.newBuilder().setSuccess(services.remove(id)).build());
        responseObserver.onCompleted();
    }

    @Override
    public void list(ListRequest request, StreamObserver<ListResponse> responseObserver) {
        responseObserver.onNext(ListResponse.newBuilder().addAllModules(services).build());
        responseObserver.onCompleted();
    }

    @Override
    public void asyncList(ListRequest request, StreamObserver<ModuleIdentifier> responseObserver) {
        services.forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void findInstances(FindInstancesRequest request, StreamObserver<FindInstancesResponse> responseObserver) {
        Predicate<ModuleIdentifier> companyPredicate;
        if (!request.getCompany().isEmpty()) {
            companyPredicate = mi -> request.getCompany().equals(mi.getCompany());
        } else {
            companyPredicate = x -> true;
        }
        Predicate<ModuleIdentifier> appPredicate;
        if (!request.getApp().isEmpty()) {
            appPredicate = mi -> request.getApp().equals(mi.getApp());
        } else {
            appPredicate = x -> true;
        }
        Predicate<ModuleIdentifier> versionPredicate;
        if (!request.getVersion().isEmpty()) {
            versionPredicate  = mi -> request.getVersion().equals(mi.getVersion());
        } else {
            versionPredicate = x -> true;
        }
        // Construct Predicate
        Predicate<ModuleIdentifier> predicate = mi ->
            companyPredicate.test(mi) &&
            appPredicate.test(mi) &&
            versionPredicate.test(mi);

        // Return filtered results
        final List<ModuleIdentifier> matches = services.stream()
            .filter(predicate)
            .collect(Collectors.toList());
        responseObserver.onNext(FindInstancesResponse.newBuilder().addAllMatches(matches).build());
        responseObserver.onCompleted();
    }
}
