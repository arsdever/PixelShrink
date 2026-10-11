#include "pixel_shrink.h"

#include <algorithm>

namespace pixel_shrink {

int clamp_quality(int quality) noexcept {
    return std::clamp(quality, 1, 100);
}

}  // namespace pixel_shrink
