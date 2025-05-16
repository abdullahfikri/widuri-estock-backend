package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.AddressModel;
import dev.mfikri.widuriestock.model.user.UserCreateRequest;
import dev.mfikri.widuriestock.model.user.UserResponse;
import dev.mfikri.widuriestock.model.user.UserUpdateRequest;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import dev.mfikri.widuriestock.util.BCrypt;
import dev.mfikri.widuriestock.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ValidationService validationService;

    public UserServiceImpl(UserRepository userRepository, AddressRepository addressRepository, ValidationService validationService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.validationService = validationService;
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        validationService.validate(request);

        if (userRepository.existsById(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());

        if (!request.getPhoto().isEmpty()){
            Path path = uploadPhoto(request.getPhoto(), request.getUsername());
            user.setPhoto(path.toString());
        }

        user.setRole(request.getRole());

        user.setDateIn(Instant.now());
        userRepository.save(user);

        if (request.getAddress() != null) {
            Address address = new Address();
            address.setStreet(request.getAddress().getStreet());
            address.setVillage(request.getAddress().getVillage());
            address.setDistrict(request.getAddress().getDistrict());
            address.setCity(request.getAddress().getCity());
            address.setProvince(request.getAddress().getProvince());
            address.setCountry(request.getAddress().getCountry());
            address.setPostalCode(request.getAddress().getPostalCode());
            address.setUser(user);
            addressRepository.save(address);

            user.setAddresses(Set.of(address));
        }

        return toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(String username) {
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must not blank.");
        }
        User user = findUserByUsername(username);
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(UserUpdateRequest request) {
        validationService.validate(request);

        User user = findUserByUsername(request.getUsername());

        if (Objects.nonNull(request.getPassword())) {
            user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        }

        if (Objects.nonNull(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }

        if (Objects.nonNull(request.getLastName())) {
            user.setLastName(request.getLastName());
        }

        if (Objects.nonNull(request.getPhone())) {
            user.setPhone(request.getPhone());
        }

        if (Objects.nonNull(request.getEmail())) {
            user.setEmail(request.getEmail());
        }

        if (!request.getPhoto().isEmpty()) {
            Path path = uploadPhoto(request.getPhoto(), request.getUsername());
            user.setPhoto(path.toString());
        }

        if (Objects.nonNull(request.getRole())){
            user.setRole(request.getRole());;
        }

        userRepository.save(user);

        return toUserResponse(user);
    }

    private Path uploadPhoto (MultipartFile photo, String username) {
        String contentType = photo.getContentType();
        if (contentType == null || !ImageUtil.isImage(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image photo is not valid");
        }
        String type = photo.getContentType().split("/")[1];

        Path path = Path.of("upload/profile-" + username + "." + type);
        try {
            photo.transferTo(path);
            return path;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Server unavailable, try again later");
        }
    }

    private User findUserByUsername (String username) {
        return userRepository.findById(username.trim()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,  "User is not found."));
    }

    private UserResponse toUserResponse (User user) {
        Set<AddressModel> addressModels = new HashSet<>();
        if (user.getAddresses() != null) {
            addressModels = user.getAddresses().stream().map(this::toAddressModel).collect(Collectors.toSet());
        }

        return UserResponse.builder()
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .photo(user.getPhoto())
                .role(user.getRole())
                .addresses(addressModels)
                .build();
    }

    private AddressModel toAddressModel (Address address) {
        return AddressModel.builder()
                .id(address.getId())
                .street(address.getStreet())
                .village(address.getVillage())
                .district(address.getDistrict())
                .city(address.getCity())
                .province(address.getProvince())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .build();
    }
}
