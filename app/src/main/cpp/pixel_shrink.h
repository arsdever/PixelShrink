#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

namespace pixel_shrink
{

using Byte = std::uint8_t;
using EncodedImage = std::vector<Byte>;

// Owns a tightly packed RGB image. JPEG bytes are decoded into this class,
// and encode_jpeg() produces a new encoded byte vector.
class Image final
{
public:
    // Decode JPEG bytes into an owned RGB image.
    // Throws std::runtime_error when the input is invalid or the JPEG codec
    // cannot decode it.
    [[nodiscard]] static Image decode_jpeg(std::span<const Byte> jpeg_bytes);

    // Take ownership of a tightly packed RGB pixel vector (three bytes per pixel).
    [[nodiscard]] static Image from_rgb(std::size_t width,
                                       std::size_t height,
                                       std::vector<Byte> rgb_pixels);

    Image(const Image&) = delete;
    Image& operator=(const Image&) = delete;
    Image(Image&&) noexcept = default;
    Image& operator=(Image&&) noexcept = default;

    // Encode this image as JPEG. Quality must be in the range 1..100.
    [[nodiscard]] EncodedImage encode_jpeg(int quality = 85) const;

    [[nodiscard]] std::size_t width() const noexcept { return width_; }
    [[nodiscard]] std::size_t height() const noexcept { return height_; }
    [[nodiscard]] std::size_t row_stride_bytes() const noexcept { return width_ * 3; }

    [[nodiscard]] std::span<Byte> pixels() noexcept { return rgb_pixels_; }
    [[nodiscard]] std::span<const Byte> pixels() const noexcept { return rgb_pixels_; }

private:
    Image(std::size_t width, std::size_t height, std::vector<Byte> rgb_pixels) noexcept;

    std::size_t width_;
    std::size_t height_;
    std::vector<Byte> rgb_pixels_;
};

// Example image operation used by the Android bridge.
void apply_red_filter(Image& image) noexcept;

} // namespace pixel_shrink
