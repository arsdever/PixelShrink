set(PYBIND11_FINDPYTHON ON)
find_package(Python COMPONENTS Interpreter Development.Module REQUIRED)
find_package(pybind11 CONFIG REQUIRED)

pybind11_add_module(ps_py pixel_shrink_pybind.cpp)
target_link_libraries(ps_py PRIVATE ps::lib)
add_library(ps::py ALIAS ps_py)
