#ifndef ANDROIDMDP_UTIL_LOGGING_HPP
#define ANDROIDMDP_UTIL_LOGGING_HPP

#ifndef MDP_LOGGING_ENABLED
#define MDP_LOGGING_ENABLED 0
#endif

#define MDP_SEVERITY_DEBUG 0
#define MDP_SEVERITY_INFO 1
#define MDP_SEVERITY_WARNING 2
#define MDP_SEVERITY_ERROR 3

#if MDP_LOGGING_ENABLED == 1

#include <sstream>

namespace AndroidMDP
{
    using MDPLoggerFun = void(int, const char*);

    inline MDPLoggerFun *MDPLogger = nullptr;

    namespace Util
    {
        inline char logBuffer[500] = {0};
    }
}

#else

namespace AndroidMDP
{
    namespace Util
    {
        struct FakeLogger
        {
            FakeLogger(int) { }

            template <typename T>
            FakeLogger &operator<<(const T&) { return *this; }
        };
    }
}

#endif

#if MDP_LOGGING_ENABLED == 1

#define MDP_LOGGER(SEV, ARGS)                               \
    do                                                      \
    {                                                       \
        if(MDPLogger)                                       \
        {                                                   \
            std::stringstream internal_stringstream_;       \
            internal_stringstream_.rdbuf()->pubsetbuf(      \
                AndroidMDP::Util::logBuffer,                \
                sizeof(AndroidMDP::Util::logBuffer) - 1     \
            );                                              \
            internal_stringstream_ << ARGS << '\0';         \
            MDPLogger(SEV, AndroidMDP::Util::logBuffer);    \
        }                                                   \
    }                                                       \
    while(0)

#else

#define MDP_LOGGER(SEV, ARGS)                               \
    while(0)                                                \
    {                                                       \
        AndroidMDP::Util::FakeLogger(SEV) << ARGS;          \
    }
#endif

#endif