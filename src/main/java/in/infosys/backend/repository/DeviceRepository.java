package in.infosys.backend.repository;

import in.infosys.backend.entity.Device;
import in.infosys.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceIdAndUser(String deviceId, User user);

    List<Device> findByUserOrderByLastSeenDesc(User user);

    Optional<Device> findByIdAndUser(Long id, User user);

    }
