cmake_minimum_required(VERSION 3.22.1)
project(pixel_shrink LANGUAGES CXX)

add_library(pixel_shrink_core STATIC pixel_shrink.cpp)
target_include_directories(pixel_shrink_core PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_features(pixel_shrink_core PUBLIC cxx_std_20)

add_library(pixel_shrink_jni SHARED pixel_shrink_jni.cpp)
target_link_libraries(pixel_shrink_jni PRIVATE pixel_shrink_core)
