#include <exception>
#include <limits>
#include <new>
#include <stdexcept>
#include <utility>
#include <vector>

#include <jni.h>

#include "pixel_shrink.h"

namespace
{

void throw_java_exception(JNIEnv* env, const char* class_name, const char* message)
{
    jclass exception_class = env->FindClass(class_name);
    if (exception_class != nullptr)
    {
        env->ThrowNew(exception_class, message);
        env->DeleteLocalRef(exception_class);
    }
}

} // namespace

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_pixelshrink_app_MainActivity_nativeProcessImage(JNIEnv* env,
                                                         jobject,
                                                         jbyteArray rgb_bytes,
                                                         jint width,
                                                         jint height,
                                                         jint quality)
{
    if (rgb_bytes == nullptr)
    {
        throw_java_exception(env, "java/lang/IllegalArgumentException", "RGB pixel data is null");
        return nullptr;
    }

    try
    {
        const jsize byte_count = env->GetArrayLength(rgb_bytes);
        if (byte_count <= 0)
        {
            throw std::invalid_argument("RGB pixel data is empty");
        }

        std::vector<pixel_shrink::Byte> pixels(static_cast<std::size_t>(byte_count));
        env->GetByteArrayRegion(rgb_bytes,
                                0,
                                byte_count,
                                reinterpret_cast<jbyte*>(pixels.data()));
        if (env->ExceptionCheck())
        {
            return nullptr;
        }

        if (width <= 0 || height <= 0)
        {
            throw std::invalid_argument("Image dimensions must be positive");
        }

        pixel_shrink::Image image = pixel_shrink::Image::from_rgb(
            static_cast<std::size_t>(width),
            static_cast<std::size_t>(height),
            std::move(pixels));

        pixel_shrink::apply_red_filter(image);
        pixel_shrink::EncodedImage encoded = image.encode_jpeg(quality);
        if (encoded.size() > static_cast<std::size_t>(std::numeric_limits<jsize>::max()))
        {
            throw std::runtime_error("Encoded image is too large to return to Java");
        }

        jbyteArray result = env->NewByteArray(static_cast<jsize>(encoded.size()));
        if (result == nullptr)
        {
            return nullptr;
        }

        env->SetByteArrayRegion(result,
                                0,
                                static_cast<jsize>(encoded.size()),
                                reinterpret_cast<const jbyte*>(encoded.data()));
        if (env->ExceptionCheck())
        {
            env->DeleteLocalRef(result);
            return nullptr;
        }
        return result;
    }
    catch (const std::invalid_argument& error)
    {
        throw_java_exception(env, "java/lang/IllegalArgumentException", error.what());
    }
    catch (const std::bad_alloc&)
    {
        throw_java_exception(env, "java/lang/OutOfMemoryError", "Not enough memory to process image");
    }
    catch (const std::exception& error)
    {
        throw_java_exception(env, "java/lang/RuntimeException", error.what());
    }
    catch (...)
    {
        throw_java_exception(env, "java/lang/RuntimeException", "Unknown native image processing error");
    }

    return nullptr;
}
