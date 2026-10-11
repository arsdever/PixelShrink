#pragma once

#include <cstdint>

namespace pixel_shrink {

// Platform-independent entry point for the image engine. Android and iOS
// adapters can call this API without pulling in platform UI dependencies.
int clamp_quality(int quality) noexcept;

}  // namespace pixel_shrink
