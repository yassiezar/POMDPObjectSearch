#include <AndroidMDP/Util/Seed.hpp>

#include <chrono>
#include <limits>

namespace AndroidMDP
{
    namespace Util
    {
        Seed Seed::instance_;

        Seed::Seed() : generator_(200)
        {} //std::chrono::system_clock::now().time_since_epoch().count()) {}

        unsigned Seed::getSeed()
        {
            static std::uniform_int_distribution<unsigned> dist(0, std::numeric_limits<unsigned>::max());

            return dist(instance_.generator_);
        }

        void Seed::setRootSeed(const unsigned seed)
        {
            instance_.generator_.seed(seed);
        }
    }
}