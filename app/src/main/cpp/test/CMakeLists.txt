find_package(GTest CONFIG REQUIRED)

add_executable(tests test_red_filter.cpp)

target_link_libraries(tests PRIVATE GTest::gtest_main ps::lib)

include(GoogleTest)
gtest_discover_tests(tests)
