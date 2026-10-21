#include <iostream>
#include <random>

#include <gtest/gtest.h>
#include <pixel_shrink.h>

TEST(ImageProcessing, RedFilter)
{
    std::vector<ps::Byte> data(10 * 10 * 3);
    std::random_device rd;

    std::transform(data.begin(),
                   data.end(),
                   data.begin(),
                   [ &rd ](const auto& _) -> ps::Byte { return rd(); });

    ps::Image img = ps::Image::from_rgb(10, 10, data);
    ps::apply_red_filter(img);

    for (int i = 0; i < data.size(); ++i)
    {
        if (i % 3 == 0)
            EXPECT_EQ(img.pixels()[ i ], data[ i ]);
        else
            EXPECT_EQ(img.pixels()[ i ], 0);
    }
}
