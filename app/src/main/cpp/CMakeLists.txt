cmake_minimum_required(VERSION 3.22.1)

project(pixel_shrink LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

add_subdirectory(src)

option(BUILD_TESTING "Build native tests" OFF)

if(BUILD_TESTING AND NOT ANDROID)
	enable_testing()
	add_subdirectory(test)
endif()
