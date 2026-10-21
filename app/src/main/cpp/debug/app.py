import sys
import argparse

def load_module():
    import ps_py
    import random
    data = random.randbytes(10 * 10 * 3)
    image = ps_py.Image(10, 10, data)
    ps_py.apply_red_filter(image)
    print(image)

def main():
    argparser = argparse.ArgumentParser()
    argparser.add_argument('--module_path', '-m')
    args = argparser.parse_args()

    sys.path.insert(0, args.module_path)
    load_module()

if __name__ == "__main__":
    main()
