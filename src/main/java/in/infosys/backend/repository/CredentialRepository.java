package in.infosys.backend.repository;

import in.infosys.backend.entity.Credential;
import in.infosys.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long> {

//    List<Credential> findAllByUser(User user);
//
//    Optional<Credential> findByIdAndUser(Long id, User user);

//    above thinks move to  soft-delete

    // Get only active credentials of a user
    List<Credential> findAllByUserAndDeletedFalse(User user);

    // Find an active credential belonging to a user
    Optional<Credential> findByIdAndUserAndDeletedFalse(
            Long id,
            User user
    );

    // Later: get deleted credentials for Trash
    List<Credential> findAllByUserAndDeletedTrue(User user);


    // Later: find deleted credential for restore
    Optional<Credential> findByIdAndUserAndDeletedTrue(
            Long id,
            User user
    );

    Optional<Credential> findByIdAndDeletedFalse(Long id);

    @Query("select c from Credential c where c.user = :user and c.deleted = false " +
           "and (:search is null or :search = '' or lower(c.title) like lower(concat('%', :search, '%')) " +
           "or lower(c.username) like lower(concat('%', :search, '%')) " +
           "or lower(c.website) like lower(concat('%', :search, '%'))) " +
           "and (:category is null or :category = '' or c.category = :category) " +
           "and (:type is null or :type = '' or c.credentialType = :type) " +
           "and (:favorite is null or c.favorite = :favorite) " +
           "order by c.title asc")
    List<Credential> searchVault(@Param("user") User user, @Param("search") String search,
                                 @Param("category") String category, @Param("type") String type,
                                 @Param("favorite") Boolean favorite);
}
