#include <algorithm>
#include <iostream>
#include <string_view>
#include <vector>

#include <pixel_shrink.h>
#include <pybind11/pybind11.h>

namespace py = pybind11;

void greet_cpp() { std::cout << "Hello Arsen" << std::endl; }

PYBIND11_MODULE(ps_py, m)
{
    m.doc() = "PixelShrink lib's python bindings"; // Optional module docstring

    py::class_<ps::Image>(m, "Image")
        // Bind the factory method as the constructor
        .def(py::init(
            [](size_t width, size_t height, std::string_view data)
    {
        const uint8_t* raw_ptr = reinterpret_cast<const uint8_t*>(data.data());
        size_t size = data.size();

        if (size != width * height * 3)
        {
            throw std::exception("Invalid data to decode");
        }

        std::vector<ps::Byte> pixel_data;
        pixel_data.resize(size);
        std::copy(data.begin(), data.end(), pixel_data.begin());

        return ps::Image::from_rgb(width, height, pixel_data);
    }))
        .def("__str__",
             [](const ps::Image& img) -> std::string
    {
        std::stringstream ss;
        ss << "Image: ";
        for (auto p : img.pixels())
        {
            ss << std::hex << static_cast<int>(p) << ' ';
        }
        return ss.str();
    });

    // Expose the function to Python
    m.def("apply_red_filter", &ps::apply_red_filter, "A function that greets");
}
