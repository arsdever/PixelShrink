add_library(ps_lib STATIC pixel_shrink.cpp)
add_library(ps::lib ALIAS ps_lib)

find_package(libjpeg-turbo REQUIRED CONFIG)

target_include_directories(ps_lib PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(ps_lib libjpeg-turbo::libjpeg-turbo)
