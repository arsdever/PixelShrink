add_library(ps_jni SHARED pixel_shrink_jni.cpp)
add_library(ps::jni ALIAS ps_jni)

target_link_libraries(ps_jni PRIVATE ps::lib)
