package ua.shykun.mysqlstresstest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
public class Api {

    private final UserRepository userRepository;

    private static final int TTL_IN_SECONDS = 10;
    private static final double PROBABILITY_DELTA = 1.0;

    private static final Random RANDOM = new Random();

    LoadingCache<String, Pair<UserEntity, LocalDateTime>> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofSeconds(TTL_IN_SECONDS))
            .build(new CacheLoader<String, Pair<UserEntity, LocalDateTime>>() {
                @Override
                public Pair<UserEntity, LocalDateTime> load(String key) {
                    return Pair.of(
                            userRepository.findFirstByEmail(key).orElse(null),
                            LocalDateTime.now().plusSeconds(TTL_IN_SECONDS));
                }
            });

    public Api(@Autowired UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private UserEntity getWithProbabilisticFlushing(String key) {
        Pair<UserEntity, LocalDateTime> cached = cache.getUnchecked(key);

        LocalDateTime now = LocalDateTime.now(); // current time
        double rand = RANDOM.nextDouble(); // random value between 0.0 and 1.0
        double randDelta = Math.floor(Math.log(rand)) * PROBABILITY_DELTA;
        if (now.minusSeconds((long) randDelta).isAfter(cached.getValue())) {
            cache.refresh(key);
        }

        return cache.getUnchecked(key).getLeft();
    }


    @GetMapping(path = "/api/generate")
    public ResponseEntity generateRandomUsers(@RequestParam(name = "count") int count,
                                              @RequestParam(name = "domain") String domain) {
        List<UserEntity> toSave = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            toSave.add(UserEntity.generateWithDomain(domain));
        }
        userRepository.saveAll(toSave);
        return ResponseEntity.ok("Ok");
    }

    @GetMapping(path = "/api/find/simple")
    public ResponseEntity findByEmail(@RequestParam(name = "email") String email) {
        Optional<UserEntity> entity = userRepository.findFirstByEmail(email);
        if (entity.isPresent()) {
            return ResponseEntity.ok(entity.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/api/find/cache")
    public ResponseEntity findByEmailWithCache(@RequestParam(name = "email") String email) {
        Pair<UserEntity, LocalDateTime> result = cache.getUnchecked(email);
        if (result != null) {
            return ResponseEntity.ok(result.getKey());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/api/find/probability-cache")
    public ResponseEntity findByEmailWithProbabilityCache(@RequestParam(name = "email") String email) {
        UserEntity userEntity = getWithProbabilisticFlushing(email);
        if (userEntity != null) {
            return ResponseEntity.ok(userEntity);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
