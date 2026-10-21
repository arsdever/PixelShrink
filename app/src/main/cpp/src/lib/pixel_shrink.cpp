#include "pixel_shrink.h"

#include <csetjmp>
#include <cstdio>
#include <cstdlib>
#include <limits>
#include <memory>
#include <new>
#include <stdexcept>
#include <utility>

#include <jpeglib.h>

namespace ps
{
namespace
{

struct JpegErrorManager
{
    jpeg_error_mgr base;
    std::jmp_buf jump_buffer;
    char message[JMSG_LENGTH_MAX];
};

struct DecodeState
{
    jpeg_decompress_struct codec;
    JpegErrorManager error;
    Byte* decoded_pixels;
    bool codec_created;
};

struct EncodeState
{
    jpeg_compress_struct codec;
    JpegErrorManager error;
    unsigned char* encoded_bytes;
    unsigned long encoded_size;
    bool codec_created;
};

void jpeg_error_exit(j_common_ptr codec) noexcept
{
    auto* error = reinterpret_cast<JpegErrorManager*>(codec->err);
    (*codec->err->format_message)(codec, error->message);
    std::longjmp(error->jump_buffer, 1);
}

void copy_error_message(const char* message, char* destination, std::size_t capacity) noexcept
{
    if (destination == nullptr || capacity == 0)
    {
        return;
    }

    const char* source = message != nullptr && message[0] != '\0'
        ? message
        : "Unknown JPEG codec error";
    std::snprintf(destination, capacity, "%s", source);
}

bool checked_rgb_size(std::size_t width, std::size_t height, std::size_t& size) noexcept
{
    if (width == 0 || height == 0 || width > std::numeric_limits<std::size_t>::max() / 3)
    {
        return false;
    }

    const std::size_t row_size = width * 3;
    if (height > std::numeric_limits<std::size_t>::max() / row_size)
    {
        return false;
    }

    size = row_size * height;
    return true;
}

bool decode_jpeg_buffer(const Byte* jpeg_bytes,
                        std::size_t jpeg_size,
                        Byte*& decoded_pixels,
                        std::size_t& width,
                        std::size_t& height,
                        char* error_message,
                        std::size_t error_capacity)
{
    decoded_pixels = nullptr;
    width = 0;
    height = 0;

    if (jpeg_bytes == nullptr || jpeg_size == 0 ||
        jpeg_size > std::numeric_limits<unsigned long>::max())
    {
        copy_error_message("JPEG input is empty or too large", error_message, error_capacity);
        return false;
    }

    auto* state = new (std::nothrow) DecodeState{};
    if (state == nullptr)
    {
        copy_error_message("Could not allocate JPEG decoder state", error_message, error_capacity);
        return false;
    }

    state->codec.err = jpeg_std_error(&state->error.base);
    state->error.base.error_exit = jpeg_error_exit;

    if (setjmp(state->error.jump_buffer) != 0)
    {
        if (state->codec_created)
        {
            jpeg_destroy_decompress(&state->codec);
        }
        std::free(state->decoded_pixels);
        copy_error_message(state->error.message, error_message, error_capacity);
        delete state;
        return false;
    }

    jpeg_create_decompress(&state->codec);
    state->codec_created = true;
    jpeg_mem_src(&state->codec,
                 reinterpret_cast<const unsigned char*>(jpeg_bytes),
                 static_cast<unsigned long>(jpeg_size));

    if (jpeg_read_header(&state->codec, TRUE) != JPEG_HEADER_OK)
    {
        copy_error_message("Invalid JPEG header", error_message, error_capacity);
        jpeg_destroy_decompress(&state->codec);
        state->codec_created = false;
        delete state;
        return false;
    }

    state->codec.out_color_space = JCS_RGB;
    if (jpeg_start_decompress(&state->codec) == FALSE || state->codec.output_components != 3)
    {
        copy_error_message("Could not start RGB JPEG decompression", error_message, error_capacity);
        jpeg_destroy_decompress(&state->codec);
        state->codec_created = false;
        delete state;
        return false;
    }

    width = static_cast<std::size_t>(state->codec.output_width);
    height = static_cast<std::size_t>(state->codec.output_height);
    std::size_t decoded_size = 0;
    if (!checked_rgb_size(width, height, decoded_size))
    {
        copy_error_message("JPEG dimensions are invalid or too large", error_message, error_capacity);
        jpeg_destroy_decompress(&state->codec);
        state->codec_created = false;
        delete state;
        width = 0;
        height = 0;
        return false;
    }

    state->decoded_pixels = static_cast<Byte*>(std::malloc(decoded_size));
    if (state->decoded_pixels == nullptr)
    {
        copy_error_message("Could not allocate decoded image pixels", error_message, error_capacity);
        jpeg_destroy_decompress(&state->codec);
        state->codec_created = false;
        delete state;
        width = 0;
        height = 0;
        return false;
    }

    const std::size_t row_stride = width * 3;
    while (state->codec.output_scanline < state->codec.output_height)
    {
        const auto row = static_cast<std::size_t>(state->codec.output_scanline);
        JSAMPROW row_pointer[1] = { state->decoded_pixels + row * row_stride };
        if (jpeg_read_scanlines(&state->codec, row_pointer, 1) != 1)
        {
            copy_error_message("Could not read JPEG scanline", error_message, error_capacity);
            jpeg_destroy_decompress(&state->codec);
            state->codec_created = false;
            std::free(state->decoded_pixels);
            state->decoded_pixels = nullptr;
            delete state;
            width = 0;
            height = 0;
            return false;
        }
    }

    if (jpeg_finish_decompress(&state->codec) == FALSE)
    {
        copy_error_message("Could not finish JPEG decompression", error_message, error_capacity);
        jpeg_destroy_decompress(&state->codec);
        state->codec_created = false;
        std::free(state->decoded_pixels);
        state->decoded_pixels = nullptr;
        delete state;
        width = 0;
        height = 0;
        return false;
    }

    jpeg_destroy_decompress(&state->codec);
    state->codec_created = false;
    decoded_pixels = state->decoded_pixels;
    state->decoded_pixels = nullptr;
    delete state;
    return true;
}

bool encode_jpeg_buffer(const Byte* rgb_pixels,
                        std::size_t width,
                        std::size_t height,
                        int quality,
                        unsigned char*& encoded_bytes,
                        std::size_t& encoded_size,
                        char* error_message,
                        std::size_t error_capacity)
{
    encoded_bytes = nullptr;
    encoded_size = 0;

    if (rgb_pixels == nullptr || width == 0 || height == 0 ||
        width > std::numeric_limits<JDIMENSION>::max() ||
        height > std::numeric_limits<JDIMENSION>::max())
    {
        copy_error_message("RGB image dimensions or pixel data are invalid", error_message, error_capacity);
        return false;
    }

    auto* state = new (std::nothrow) EncodeState{};
    if (state == nullptr)
    {
        copy_error_message("Could not allocate JPEG encoder state", error_message, error_capacity);
        return false;
    }

    state->codec.err = jpeg_std_error(&state->error.base);
    state->error.base.error_exit = jpeg_error_exit;

    if (setjmp(state->error.jump_buffer) != 0)
    {
        if (state->codec_created)
        {
            jpeg_destroy_compress(&state->codec);
        }
        std::free(state->encoded_bytes);
        copy_error_message(state->error.message, error_message, error_capacity);
        delete state;
        return false;
    }

    jpeg_create_compress(&state->codec);
    state->codec_created = true;
    jpeg_mem_dest(&state->codec, &state->encoded_bytes, &state->encoded_size);

    state->codec.image_width = static_cast<JDIMENSION>(width);
    state->codec.image_height = static_cast<JDIMENSION>(height);
    state->codec.input_components = 3;
    state->codec.in_color_space = JCS_RGB;

    jpeg_set_defaults(&state->codec);
    jpeg_set_quality(&state->codec, quality, TRUE);
    jpeg_start_compress(&state->codec, TRUE);

    const std::size_t row_stride = width * 3;
    while (state->codec.next_scanline < state->codec.image_height)
    {
        const auto row = static_cast<std::size_t>(state->codec.next_scanline);
        JSAMPROW row_pointer[1] = {
            const_cast<Byte*>(rgb_pixels + row * row_stride)
        };
        if (jpeg_write_scanlines(&state->codec, row_pointer, 1) != 1)
        {
            copy_error_message("Could not write JPEG scanline", error_message, error_capacity);
            jpeg_destroy_compress(&state->codec);
            state->codec_created = false;
            std::free(state->encoded_bytes);
            state->encoded_bytes = nullptr;
            delete state;
            return false;
        }
    }

    jpeg_finish_compress(&state->codec);
    jpeg_destroy_compress(&state->codec);
    state->codec_created = false;

    encoded_bytes = state->encoded_bytes;
    encoded_size = static_cast<std::size_t>(state->encoded_size);
    state->encoded_bytes = nullptr;
    delete state;
    return true;
}

struct FreeDeleter
{
    void operator()(Byte* memory) const noexcept { std::free(memory); }
};

struct UnsignedCharFreeDeleter
{
    void operator()(unsigned char* memory) const noexcept { std::free(memory); }
};

} // namespace

Image::Image(std::size_t width, std::size_t height, std::vector<Byte> rgb_pixels) noexcept
    : width_(width), height_(height), rgb_pixels_(std::move(rgb_pixels))
{
}

Image Image::decode_jpeg(std::span<const Byte> jpeg_bytes)
{
    Byte* decoded_pixels = nullptr;
    std::size_t decoded_width = 0;
    std::size_t decoded_height = 0;
    char error_message[JMSG_LENGTH_MAX] = {};

    if (!decode_jpeg_buffer(jpeg_bytes.data(),
                            jpeg_bytes.size(),
                            decoded_pixels,
                            decoded_width,
                            decoded_height,
                            error_message,
                            sizeof(error_message)))
    {
        throw std::runtime_error(error_message);
    }

    std::unique_ptr<Byte, FreeDeleter> owned_pixels(decoded_pixels);
    const std::size_t decoded_size = decoded_width * decoded_height * 3;
    std::vector<Byte> pixels(owned_pixels.get(), owned_pixels.get() + decoded_size);
    return Image(decoded_width, decoded_height, std::move(pixels));
}

Image Image::from_rgb(std::size_t width,
                      std::size_t height,
                      std::vector<Byte> rgb_pixels)
{
    std::size_t expected_size = 0;
    if (!checked_rgb_size(width, height, expected_size) || rgb_pixels.size() != expected_size)
    {
        throw std::invalid_argument("RGB pixel vector size does not match the image dimensions");
    }

    return Image(width, height, std::move(rgb_pixels));
}

EncodedImage Image::encode_jpeg(int quality) const
{
    if (quality < 1 || quality > 100)
    {
        throw std::invalid_argument("JPEG quality must be in the range 1..100");
    }

    std::size_t expected_size = 0;
    if (!checked_rgb_size(width_, height_, expected_size) || rgb_pixels_.size() != expected_size)
    {
        throw std::runtime_error("Image has invalid RGB pixel storage");
    }

    unsigned char* encoded_bytes = nullptr;
    std::size_t encoded_size = 0;
    char error_message[JMSG_LENGTH_MAX] = {};
    if (!encode_jpeg_buffer(rgb_pixels_.data(),
                            width_,
                            height_,
                            quality,
                            encoded_bytes,
                            encoded_size,
                            error_message,
                            sizeof(error_message)))
    {
        throw std::runtime_error(error_message);
    }

    std::unique_ptr<unsigned char, UnsignedCharFreeDeleter> owned_bytes(encoded_bytes);
    return EncodedImage(owned_bytes.get(), owned_bytes.get() + encoded_size);
}

void apply_red_filter(Image& image) noexcept
{
    std::span<Byte> pixels = image.pixels();
    for (std::size_t offset = 0; offset + 2 < pixels.size(); offset += 3)
    {
        pixels[offset + 1] = 0;
        pixels[offset + 2] = 0;
    }
}

} // namespace pixel_shrink
