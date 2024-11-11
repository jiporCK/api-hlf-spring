package anuznomii.lol.apihyperledgerfabricspring.restcontrollers;

import anuznomii.lol.apihyperledgerfabricspring.models.CAEnrollmentRequest;
import anuznomii.lol.apihyperledgerfabricspring.services.FabricCAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/enrollment")
public class FabricCAController {

    private final FabricCAService fabricCAService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody CAEnrollmentRequest request) {
        try {
            fabricCAService.registerAndEnrollAdminUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    "User registered and enrolled successfully"
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

}
