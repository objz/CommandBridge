# testing

the test suite covers the core scripting engine, security, networking primitives, the velocity dispatch pipeline, and backend platform utilities. it does not cover platform-specific Bukkit/Paper/Folia APIs (runtime-only) or the PlaceholderAPI integration path in `PlaceholderStage` (also runtime-only). no Mockito, all test doubles are hand-written.

## running tests

```sh
# run all tests across all modules
./gradlew test

# run tests for a single module
./gradlew :core:test
./gradlew :velocity:test
./gradlew :backends:test
./gradlew :backends:velocity:test

# run a single test class
./gradlew :core:test --tests "dev.objz.commandbridge.security.AuthServiceTest"

# run a single test method
./gradlew :core:test --tests "dev.objz.commandbridge.security.AuthServiceTest.signValidInputReturnsDeterministicOutput"

# run checkstyle + all tests (what CI runs)
./gradlew check
```

---

## conventions

every test class must follow these rules — checkstyle runs on test sources with the same config as main sources.

- class is `final`, package-private (no `public` modifier)
- class has a one-line JavaDoc comment
- `@BeforeAll` calls `TestFixtures.ensureLog()` to install the logging facade
- method names are camelCase — no underscores (`^[a-z][a-zA-Z0-9]*$` is enforced)
- no wildcard imports
- 4-space indentation, no tabs

example skeleton:

```java
/** Tests for MyThing. */
final class MyThingTest {

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void returnsExpectedValue() {
        // ...
    }
}
```

---

## test organization

| module | tests | what's covered |
|---|---|---|
| `core` | ~84 | auth, rate limiting, scripting validation, argument adapters, placeholder extraction, envelope serialization, response awaiter |
| `velocity` | ~40 | dispatch pipeline stages, command dispatcher, cooldown manager, player tracker |
| `backends` | ~7 | platform detection, path normalization |
| `backends:velocity` | ~5 | permissible command source |

test sources live at `<module>/src/test/java/` following standard Gradle layout. each module has a `TestFixtures` class with shared setup helpers.

---

## adding tests

1. create your class in the correct module under `src/test/java/`
2. match the package of the class under test
3. follow the conventions above (final, package-private, JavaDoc, `ensureLog()`)
4. if you need a fake platform object, add it to the module's `TestFixtures` or write a hand-written double — no Mockito
5. run `./gradlew :module:test` to verify, then `./gradlew check` before opening a PR

