#ifndef ANDROIDMDP_UTIL_SEED_HPP
#define ANDROIDMDP_UTIL_SEED_HPP

#include <random>

namespace AndroidMDP
{
    namespace Util
    {
        class Seed
        {
        private:
            Seed();

            static Seed instance_;

            std::default_random_engine generator_;

        public:
            static unsigned getSeed();

            static void setRootSeed(unsigned seed);
        };
    }
}

#endif
