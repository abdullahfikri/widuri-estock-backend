package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.Supplier;
import dev.mfikri.widuriestock.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findAllByUser(User user);

    List<Address> findAllByUser(User user, Sort sort);

    Optional<Address> findAddressByIdAndUser_Username(int id, String userUsername);

    Optional<Address> findAddressByIdAndUser(int id, User user);

    int deleteAddressByIdAndUser(int id, User user);

    void deleteAllBySupplier(Supplier supplier);

    Address findBySupplier(Supplier supplier);
}
