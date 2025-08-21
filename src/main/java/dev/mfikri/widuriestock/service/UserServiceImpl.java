package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.address.AddressCreateRequest;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.user.*;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.BCrypt;
import dev.mfikri.widuriestock.util.ImageUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ValidationService validationService;
    private final AddressService addressService;

    public UserServiceImpl(UserRepository userRepository, ValidationService validationService, AddressService addressService) {
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.addressService = addressService;
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        log.info("Processing create user request, username={}", request.getUsername());
        validationService.validate(request);

        if (userRepository.existsById(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered. Please use a different username.");
        }

        User user = buildUser(request);
        if (request.getAddress() != null) {
            log.debug("Building address. address={}", request.getAddress());
            AddressCreateRequest requestAddress = request.getAddress();
            Address address = new Address();
            addressService.setAddress(address,
                    requestAddress.getStreet(),
                    requestAddress.getVillage(),
                    requestAddress.getDistrict(),
                    requestAddress.getCity(),
                    requestAddress.getProvince(),
                    requestAddress.getCountry(),
                    requestAddress.getPostalCode()
                    );
            address.setUser(user);
            user.setAddresses(Set.of(address));
        }

        log.debug("Saving user to database.");
        User savedUser = userRepository.save(user);

        log.info("Successfully created user, username={}", request.getUsername());
        return toUserResponse(savedUser);
    }

    private User buildUser(UserCreateRequest request) {
        log.debug("Building new user, username={}", request.getUsername());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword("{bcrypt}" + BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());

        if (!request.getPhoto().isEmpty()){
            Path path = ImageUtil.uploadPhoto(request.getPhoto(), request.getUsername(), false);
            user.setPhoto(path.toString());
        }

        user.setRole(request.getRole().toUpperCase());

        user.setDateIn(Instant.now());

        return user;
    }



    @Override
    @Transactional(readOnly = true)
    public UserResponse get(String username) {
        log.info("Processing to get a user data, username={}", username);

        User user = findUserByUsernameOrThrows(username);

        log.info("Successfully get a user data");
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(UserUpdateRequest request, boolean isCurrentUser) {
        log.info("Processing to update user data. username={}, isCurrentUser={}", request.getUsername(), isCurrentUser);
        validationService.validate(request);

        User user = findUserByUsernameOrThrows(request.getUsername());

        applyUpdatesToUser(request, user, isCurrentUser);

        log.info("Successfully updated user data. username={}", request.getUsername());
        return toUserResponse(user);
    }

    private void applyUpdatesToUser(UserUpdateRequest request, User user, boolean isCurrentUser) {
        log.debug("Applying updates to user. username={}", request.getUsername());
        Optional.ofNullable(request.getPassword())
                .filter(pw -> !pw.isBlank())
                .ifPresent( pw -> user.setPassword("{bcrypt}" + BCrypt.hashpw(request.getPassword(), BCrypt.gensalt())));

        Optional.ofNullable(request.getFirstName()).ifPresent(user::setFirstName);
        Optional.ofNullable(request.getLastName()).ifPresent(user::setLastName);
        Optional.ofNullable(request.getPhone()).ifPresent(user::setPhone);
        Optional.ofNullable(request.getEmail()).ifPresent(user::setEmail);

        Optional.ofNullable(request.getPhoto())
                .filter(photo -> !photo.isEmpty())
                .ifPresent(photo -> {
                    Path path = ImageUtil.uploadPhoto(request.getPhoto(), request.getUsername(), false);
                    user.setPhoto(path.toString());
                });

        if (!isCurrentUser && request.getRole() != null) {
            user.setRole(request.getRole());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Page<UserSearchResponse> searchUser(UserSearchFilterRequest filterRequest) {
        validationService.validate(filterRequest);

        Specification<User> specification = createSearchSpecification(filterRequest);

        Pageable pageable= PageRequest.of(filterRequest.getPage(), filterRequest.getSize());

        Page<User> userPage = userRepository.findAll(specification, pageable);

        return userPage.map(this::toUserSearchResponse);
    }

    private Specification<User> createSearchSpecification(UserSearchFilterRequest filterRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterRequest.getUsername() != null && !filterRequest.getUsername().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("username"), "%" + filterRequest.getUsername() + "%"));
            }

            if (filterRequest.getName() != null && !filterRequest.getName().isBlank()) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("firstName"), "%" + filterRequest.getName() + "%"),
                        criteriaBuilder.like(root.get("lastName"), "%" + filterRequest.getName() + "%")
                ));
            }

            if (filterRequest.getPhone() != null && !filterRequest.getPhone().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("phone"), "%" + filterRequest.getPhone() + "%"));
            }

            if (filterRequest.getEmail() != null && !filterRequest.getEmail().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("email"), "%" + filterRequest.getEmail() + "%"));
            }

            if (filterRequest.getRole() != null && !filterRequest.getRole().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("role") , "%" + filterRequest.getRole() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
        };
    }

    private User findUserByUsernameOrThrows(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must not blank.");
        }
        return userRepository.findById(username.trim()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,  "User is not found."));
    }

    private UserResponse toUserResponse(User user) {
        Set<AddressResponse> addressResponses = new HashSet<>();
        if (user.getAddresses() != null) {
            addressResponses = user.getAddresses().stream().map(addressService::toAddressResponse).collect(Collectors.toSet());
        }

        return UserResponse.builder()
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .photo(user.getPhoto())
                .role(user.getRole())
                .addresses(addressResponses)
                .build();
    }

    private UserSearchResponse toUserSearchResponse(User user) {
        return UserSearchResponse.builder()
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .photo(user.getPhoto())
                .role(user.getRole())
                .build();
    };
}
