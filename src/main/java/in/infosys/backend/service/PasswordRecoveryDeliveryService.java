package in.infosys.backend.service;

public interface PasswordRecoveryDeliveryService {
    void deliver(String destination, String token);
}