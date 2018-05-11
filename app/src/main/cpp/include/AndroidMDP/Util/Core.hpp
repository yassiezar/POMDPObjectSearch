#ifndef ANDROIDMDP_UTIL_CORE_HPP
#define ANDROIDMDP_UTIL_CORE_HPP

#include <cstddef>
#include <cmath>
#include <limits>
#include <algorithm>

#include <AndroidMDP/Types.hpp>

namespace AndroidMDP
{
    namespace Util
    {
        constexpr auto equalEpsilonSmall = 0.000001;
        constexpr auto equalEpsilonGeneral = 0.000000000001;

        inline bool checkEqualSmall(const double a, const double b)
        {
            return (std::fabs(a - b) <= equalEpsilonSmall);
        }

        inline bool checkDifferentSmall(const double a, const double b)
        {
            return !(checkEqualSmall(a, b));
        }

        inline bool checkEqualGeneral(const double a, const double b)
        {
            if (checkEqualSmall(a, b)) return true;
            return (std::fabs(a - b) <= std::min(std::fabs(a), std::fabs(b)) * equalEpsilonGeneral);
        }

        inline bool checkDifferentGeneral(const double a, const double b)
        {
            return !checkEqualGeneral(a, b);
        }
    }
}

#endif
